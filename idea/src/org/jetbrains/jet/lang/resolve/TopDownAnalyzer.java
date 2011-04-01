package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetControlFlowProcessor;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowInstructionsGenerator;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    private final Map<JetClass, MutableClassDescriptor> classes = new LinkedHashMap<JetClass, MutableClassDescriptor>();
    private final Map<JetNamespace, WritableScope> namespaceScopes = new LinkedHashMap<JetNamespace, WritableScope>();
    private final Map<JetDeclaration, FunctionDescriptor> functions = new HashMap<JetDeclaration, FunctionDescriptor>();
    private final Map<JetDeclaration, WritableScope> declaringScopes = new HashMap<JetDeclaration, WritableScope>();

    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;
    private final BindingTrace trace;
    private final JetTypeInferrer typeInferrer;

    public TopDownAnalyzer(JetSemanticServices semanticServices, @NotNull BindingTrace bindingTrace) {
        this.semanticServices = semanticServices;
        this.classDescriptorResolver = new ClassDescriptorResolver(semanticServices, bindingTrace);
        this.trace = bindingTrace;
        this.typeInferrer = semanticServices.getTypeInferrer(trace);
    }

    public void process(@NotNull JetScope outerScope, @NotNull JetDeclaration declaration) {
        process(outerScope, Collections.singletonList(declaration));
    }

    public void process(@NotNull JetScope outerScope, @NotNull List<JetDeclaration> declarations) {
        final WritableScope toplevelScope = semanticServices.createWritableScope(outerScope, outerScope.getContainingDeclaration()); // TODO ?!
        trace.setToplevelScope(toplevelScope); // TODO : this is a hack
        collectTypeDeclarators(toplevelScope, declarations);
        resolveTypeDeclarations();
        processBehaviorDeclarators(toplevelScope, declarations);
        resolveBehaviorDeclarationBodies();
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void collectTypeDeclarators(
            @NotNull final WritableScope declaringScope,
            List<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
                @Override
                public void visitClass(JetClass klass) {
                    WritableScope classScope = processClass(declaringScope, klass);
                    collectTypeDeclarators(classScope, klass.getDeclarations());
                }

                @Override
                public void visitNamespace(JetNamespace namespace) {
                    List<JetImportDirective> importDirectives = namespace.getImportDirectives();

                    String name = namespace.getName();
                    if (name == null) {
                        name = "<no name provided>";
                    }
                    NamespaceDescriptor namespaceDescriptor = declaringScope.getDeclaredNamespace(name);
                    if (namespaceDescriptor == null) {
                        namespaceDescriptor = new NamespaceDescriptor(
                                declaringScope.getContainingDeclaration(),
                                Collections.<Attribute>emptyList(), // TODO
                                name
                        );
                        declaringScope.addNamespace(namespaceDescriptor);
                        trace.recordDeclarationResolution(namespace, namespaceDescriptor);
                    }

                    WritableScope namespaceScope = semanticServices.createWritableScope(declaringScope, namespaceDescriptor);
                    namespaceScopes.put(namespace, namespaceScope);

                    for (JetImportDirective importDirective : importDirectives) {
                        if (importDirective.isAbsoluteInRootNamespace()) {
                            throw new UnsupportedOperationException();
                        }
                        if (importDirective.isAllUnder()) {
                            JetExpression importedReference = importDirective.getImportedReference();
                            if (importedReference != null) {
                                JetType type = typeInferrer.getType(namespaceScope, importedReference, false);
                                if (type != null) {
                                    namespaceScope.importScope(type.getMemberScope());
                                }
                            }
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }

                    collectTypeDeclarators(namespaceScope, namespace.getDeclarations());
                }

                @Override
                public void visitTypedef(JetTypedef typedef) {
                    processTypeDef(typedef);
                }

                @Override
                public void visitDeclaration(JetDeclaration dcl) {
                    // Other declarations do not declare visible types
                }
            });
        }
    }

    private WritableScope processClass(@NotNull WritableScope declaringScope, JetClass klass) {
        MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(semanticServices, declaringScope.getContainingDeclaration(), declaringScope);
        mutableClassDescriptor.setName(klass.getName());

        declaringScope.addClassifierDescriptor(mutableClassDescriptor);

        classes.put(klass, mutableClassDescriptor);
        declaringScopes.put(klass, declaringScope);

        return mutableClassDescriptor.getUnsubstitutedMemberScope();
    }

    private void processExtension(JetExtension extension) {
        throw new UnsupportedOperationException(extension.getText()); // TODO
    }

    private void processTypeDef(@NotNull JetTypedef typedef) {
        throw new UnsupportedOperationException(typedef.getText()); // TODO
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resolveTypeDeclarations() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveMutableClassDescriptor(declaringScopes.get(jetClass), jetClass, descriptor);
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void processBehaviorDeclarators(@NotNull final WritableScope declaringScope, List<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
                @Override
                public void visitClass(JetClass klass) {
                    MutableClassDescriptor mutableClassDescriptor = classes.get(klass);
                    processPrimaryConstructor(mutableClassDescriptor, klass);
                    processBehaviorDeclarators(mutableClassDescriptor.getUnsubstitutedMemberScope(), klass.getDeclarations());
                }

                @Override
                public void visitClassObject(JetClassObject classObject) {
                    processClassObject(classObject);
                    processBehaviorDeclarators(declaringScope, classObject.getObject().getDeclarations());
                }

                @Override
                public void visitNamespace(JetNamespace namespace) {
                    WritableScope namespaceScope = namespaceScopes.get(namespace);
                    processBehaviorDeclarators(namespaceScope, namespace.getDeclarations());
                }

                @Override
                public void visitFunction(JetFunction function) {
                    processFunction(declaringScope, function);
                }

                @Override
                public void visitProperty(JetProperty property) {
                    processProperty(declaringScope, property);
                }

                @Override
                public void visitConstructor(JetConstructor constructor) {
                    DeclarationDescriptor containingDeclaration = declaringScope.getContainingDeclaration();
                    if (containingDeclaration instanceof ClassDescriptor) {
                        processConstructor((MutableClassDescriptor) containingDeclaration, constructor);
                    }
                    else {
                        semanticServices.getErrorHandler().genericError(constructor.getNode(), "Constructors are only allowed inside classes");
                    }
                }

                @Override
                public void visitDeclaration(JetDeclaration dcl) {
                    semanticServices.getErrorHandler().genericError(dcl.getNode(), "Unsupported declaration: " + dcl); // TODO
                }
            });
        }

    }

    private void processPrimaryConstructor(MutableClassDescriptor classDescriptor, JetClass klass) {
        // TODO : not all the parameters are real properties
        WritableScope memberScope = classDescriptor.getUnsubstitutedMemberScope(); // TODO : this is REALLY questionable
        ConstructorDescriptor constructorDescriptor = classDescriptorResolver.resolvePrimaryConstructor(memberScope, classDescriptor, klass);
        for (JetParameter parameter : klass.getPrimaryConstructorParameters()) {
            PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolvePrimaryConstructorParameterToAProperty(
                    classDescriptor,
                    memberScope,
                    parameter
            );
            memberScope.addPropertyDescriptor(
                    propertyDescriptor);
        }
        if (constructorDescriptor != null) {
            classDescriptor.addConstructor(constructorDescriptor);
        }
    }

    private void processConstructor(MutableClassDescriptor classDescriptor, JetConstructor constructor) {
        ConstructorDescriptor constructorDescriptor = classDescriptorResolver.resolveConstructorDescriptor(classDescriptor.getUnsubstitutedMemberScope(), classDescriptor, constructor, false);
        classDescriptor.addConstructor(constructorDescriptor);
        functions.put(constructor, constructorDescriptor);
        declaringScopes.put(constructor, classDescriptor.getUnsubstitutedMemberScope());
    }

    private void processFunction(@NotNull WritableScope declaringScope, JetFunction function) {
        declaringScopes.put(function, declaringScope);
        FunctionDescriptor descriptor = classDescriptorResolver.resolveFunctionDescriptor(declaringScope.getContainingDeclaration(), declaringScope, function);
        declaringScope.addFunctionDescriptor(descriptor);
        functions.put(function, descriptor);

        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression != null) {
            System.out.println("-------------");
            JetControlFlowInstructionsGenerator instructionsGenerator = new JetControlFlowInstructionsGenerator(function);
            new JetControlFlowProcessor(semanticServices, trace, instructionsGenerator).generate(function, bodyExpression);
            Pseudocode pseudocode = instructionsGenerator.getPseudocode();
            pseudocode.postProcess();
            pseudocode.dumpInstructions(System.out);
            System.out.println("-------------");
            try {
                pseudocode.dumpGraph(new PrintStream("/Users/abreslav/work/cfg.dot"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private void processProperty(WritableScope declaringScope, JetProperty property) {
        declaringScopes.put(property, declaringScope);
        PropertyDescriptor descriptor = classDescriptorResolver.resolvePropertyDescriptor(declaringScope.getContainingDeclaration(), declaringScope, property);
        declaringScope.addPropertyDescriptor(descriptor);
    }

    private void processClassObject(JetClassObject classObject) {
        throw new UnsupportedOperationException(); // TODO
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resolveBehaviorDeclarationBodies() {
        for (Map.Entry<JetDeclaration, FunctionDescriptor> entry : functions.entrySet()) {
            JetDeclaration declarations = entry.getKey();
            FunctionDescriptor descriptor = entry.getValue();

            WritableScope declaringScope = declaringScopes.get(declarations);
            assert declaringScope != null;

            WritableScope parameterScope = semanticServices.createWritableScope(declaringScope, descriptor);
            for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
                parameterScope.addTypeParameterDescriptor(typeParameter);
            }
            for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getUnsubstitutedValueParameters()) {
                parameterScope.addPropertyDescriptor(valueParameterDescriptor);
            }

            assert declarations instanceof JetFunction || declarations instanceof JetConstructor;
            JetExpression bodyExpression = ((JetDeclarationWithBody) declarations).getBodyExpression();
            if (bodyExpression != null) {
                resolveExpression(parameterScope, bodyExpression, true);
            }
        }
    }

    private void resolveExpression(@NotNull JetScope scope, JetExpression expression, boolean preferBlock) {
        typeInferrer.getType(scope, expression, preferBlock);
    }

}
