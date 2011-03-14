package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    private final Map<JetClass, MutableClassDescriptor> classes = new LinkedHashMap<JetClass, MutableClassDescriptor>();
    private final Map<JetNamespace, WritableScope> namespaceScopes = new LinkedHashMap<JetNamespace, WritableScope>();
    private final Map<JetFunction, FunctionDescriptor> functions = new HashMap<JetFunction, FunctionDescriptor>();
    private final Map<JetDeclaration, WritableScope> declaringScopes = new HashMap<JetDeclaration, WritableScope>();

    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;
    private final BindingTrace trace;

    public TopDownAnalyzer(JetSemanticServices semanticServices, @NotNull BindingTrace bindingTrace) {
        this.semanticServices = semanticServices;
        this.classDescriptorResolver = new ClassDescriptorResolver(semanticServices, bindingTrace);
        this.trace = bindingTrace;
    }

    public void process(@NotNull JetScope outerScope, @NotNull JetDeclaration declaration) {
        process(outerScope, Collections.singletonList(declaration));
    }

    public void process(@NotNull JetScope outerScope, @NotNull List<JetDeclaration> declarations) {
        final WritableScope toplevelScope = new WritableScope(outerScope, outerScope.getContainingDeclaration()); // TODO ?!
        trace.setToplevelScope(toplevelScope); // TODO : this is a hack
        collectTypeDeclarators(toplevelScope, declarations);
        resolveTypeDeclarations();
        collectBehaviorDeclarators(toplevelScope, declarations);
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

                    WritableScope namespaceScope = new WritableScope(declaringScope, namespaceDescriptor);
                    namespaceScopes.put(namespace, namespaceScope);

                    for (JetImportDirective importDirective : importDirectives) {
                        if (importDirective.isAbsoluteInRootNamespace()) {
                            throw new UnsupportedOperationException();
                        }
                        if (importDirective.isAllUnder()) {
                            JetExpression importedReference = importDirective.getImportedReference();
                            Type type = semanticServices.getTypeInferrer(trace).getType(namespaceScope, importedReference, false);
                            if (type != null) {
                                namespaceScope.importScope(type.getMemberScope());
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
                    throw new UnsupportedOperationException(); // TODO
                }
            });
        }
    }

    private WritableScope processClass(@NotNull WritableScope declaringScope, JetClass klass) {
        MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(declaringScope.getContainingDeclaration(), declaringScope);
        mutableClassDescriptor.setName(klass.getName());

        declaringScope.addClassDescriptor(mutableClassDescriptor);

        classes.put(klass, mutableClassDescriptor);
        trace.recordDeclarationResolution(klass, mutableClassDescriptor);
        declaringScopes.put(klass, declaringScope);

        return mutableClassDescriptor.getUnsubstitutedMemberScope();
    }

    private void processExtension(JetExtension extension) {
        throw new UnsupportedOperationException(); // TODO
    }

    private void processTypeDef(JetTypedef typedef) {
        throw new UnsupportedOperationException(); // TODO
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

    private void collectBehaviorDeclarators(@NotNull final WritableScope declaringScope, List<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
                @Override
                public void visitClass(JetClass klass) {
                    collectBehaviorDeclarators(classes.get(klass).getUnsubstitutedMemberScope(), klass.getDeclarations());
                }

                @Override
                public void visitClassObject(JetClassObject classObject) {
                    processClassObject(classObject);
                    collectBehaviorDeclarators(declaringScope, classObject.getObject().getDeclarations());
                }

                @Override
                public void visitNamespace(JetNamespace namespace) {
                    WritableScope namespaceScope = namespaceScopes.get(namespace);
                    collectBehaviorDeclarators(namespaceScope, namespace.getDeclarations());
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
                public void visitDeclaration(JetDeclaration dcl) {
                    throw new UnsupportedOperationException(); // TODO
                }
            });
        }

    }

    private void processFunction(@NotNull WritableScope declaringScope, JetFunction function) {
        declaringScopes.put(function, declaringScope);
        FunctionDescriptor descriptor = classDescriptorResolver.resolveFunctionDescriptor(declaringScope.getContainingDeclaration(), declaringScope, function);
        declaringScope.addFunctionDescriptor(descriptor);
        functions.put(function, descriptor);
        trace.recordDeclarationResolution(function, descriptor);
    }

    private void processProperty(WritableScope declaringScope, JetProperty property) {
        declaringScopes.put(property, declaringScope);
        PropertyDescriptor descriptor = classDescriptorResolver.resolvePropertyDescriptor(declaringScope.getContainingDeclaration(), declaringScope, property);
        declaringScope.addPropertyDescriptor(descriptor);
        trace.recordDeclarationResolution(property, descriptor);
    }

    private void processClassObject(JetClassObject classObject) {
        throw new UnsupportedOperationException(); // TODO
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resolveBehaviorDeclarationBodies() {
        for (Map.Entry<JetFunction, FunctionDescriptor> entry : functions.entrySet()) {
            JetFunction function = entry.getKey();
            FunctionDescriptor descriptor = entry.getValue();

            WritableScope declaringScope = declaringScopes.get(function);
            assert declaringScope != null;

            WritableScope parameterScope = new WritableScope(declaringScope, descriptor);
            for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
                parameterScope.addTypeParameterDescriptor(typeParameter);
            }
            for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getUnsubstitutedValueParameters()) {
                parameterScope.addPropertyDescriptor(valueParameterDescriptor);
            }

            JetExpression bodyExpression = function.getBodyExpression();
            if (bodyExpression != null) {
                resolveExpression(parameterScope, bodyExpression, true);
            }
        }
    }

    private void resolveExpression(@NotNull JetScope scope, JetExpression expression, boolean preferBlock) {
        semanticServices.getTypeInferrer(trace).getType(scope, expression, preferBlock);
    }

}
