package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Stepan Koltsov
 */
class JavaDescriptorResolverHelper {
    
    
    private static class Builder {
        private final PsiClassWrapper psiClass;
        private final boolean staticMembers;
        private final boolean kotlin;
        
        private Map<String, NamedMembers> namedMembersMap = new HashMap<String, NamedMembers>();

        private Builder(PsiClassWrapper psiClass, boolean staticMembers, boolean kotlin) {
            this.psiClass = psiClass;
            this.staticMembers = staticMembers;
            this.kotlin = kotlin;
        }

        public void run() {
            processFields();
            processMethods();
        }
        
        private NamedMembers getNamedMembers(String name) {
            NamedMembers r = namedMembersMap.get(name);
            if (r == null) {
                r = new NamedMembers();
                r.name = name;
                namedMembersMap.put(name, r);
            }
            return r;
        }
        
        private void processFields() {
            if (!kotlin) {
                for (PsiFieldWrapper field : psiClass.getFields()) {
                    if (field.isStatic() != staticMembers) {
                        continue;
                    }

                    if (field.isPrivate()) {
                        continue;
                    }

                    NamedMembers namedMembers = getNamedMembers(field.getName());

                    MembersForProperty members = new MembersForProperty();
                    members.field = field;
                    members.type = field.getType();

                    namedMembers.properties = members;
                }
            }
        }

        private void processMethods() {
            for (HierarchicalMethodSignature method0 : psiClass.getPsiClass().getVisibleSignatures()) {
                
                PsiMethodWrapper method = new PsiMethodWrapper(method0.getMethod());

                if (method.isStatic() != staticMembers) {
                    continue;
                }

                if (method.isPrivate()) {
                    continue;
                }

                // TODO: "is" prefix
                // TODO: remove getJavaClass
                if (method.getName().startsWith(JvmAbi.GETTER_PREFIX)) {

                    // TODO: some java properties too
                    if (method.getJetMethod().kind() == JvmStdlibNames.JET_METHOD_KIND_PROPERTY) {

                        if (method.getName().equals(JvmStdlibNames.JET_OBJECT_GET_TYPEINFO_METHOD)) {
                            continue;
                        }

                        int i = 0;

                        PsiType receiverType;
                        if (i < method.getParameters().size() && method.getParameter(i).getJetValueParameter().receiver()) {
                            receiverType = method.getParameter(i).getPsiParameter().getType();
                            ++i;
                        } else {
                            receiverType = null;
                        }

                        while (i < method.getParameters().size() && method.getParameter(i).getJetTypeParameter().isDefined()) {
                            // TODO: store is reified
                            ++i;
                        }

                        if (i != method.getParameters().size()) {
                            // TODO: report error properly
                            throw new IllegalStateException();
                        }

                        String propertyName = StringUtil.decapitalize(method.getName().substring(JvmAbi.GETTER_PREFIX.length()));
                        NamedMembers members = getNamedMembers(propertyName);
                        members.getForProperty().getter = method;

                        // TODO: check conflicts with setter
                        // TODO: what if returnType == null?
                        members.getForProperty().type = method.getReturnType();
                        members.getForProperty().receiverType = receiverType;
                    }
                } else if (method.getName().startsWith(JvmAbi.SETTER_PREFIX)) {

                    if (method.getJetMethod().kind() == JvmStdlibNames.JET_METHOD_KIND_PROPERTY) {
                        if (method.getParameters().size() == 0) {
                            // TODO: report error properly
                            throw new IllegalStateException();
                        }

                        int i = 0;

                        PsiType receiverType = null;
                        PsiParameterWrapper p1 = method.getParameter(0);
                        if (p1.getJetValueParameter().receiver()) {
                            receiverType = p1.getPsiParameter().getType();
                            ++i;
                        }

                        while (i < method.getParameters().size() && method.getParameter(i).getJetTypeParameter().isDefined()) {
                            ++i;
                        }

                        if (i + 1 != method.getParameters().size()) {
                            throw new IllegalStateException();
                        }

                        PsiType propertyType = method.getParameter(i).getPsiParameter().getType();

                        String propertyName = StringUtil.decapitalize(method.getName().substring(JvmAbi.SETTER_PREFIX.length()));
                        NamedMembers members = getNamedMembers(propertyName);
                        members.getForProperty().setter = method;

                        // TODO: check conflicts with getter
                        members.getForProperty().type = propertyType;
                        members.getForProperty().receiverType = receiverType;
                    }
                }
                
                if (method.getJetMethod().kind() != JvmStdlibNames.JET_METHOD_KIND_PROPERTY) {
                    NamedMembers namedMembers = getNamedMembers(method.getName());
                    namedMembers.addMethod(method);
                }
            }
        }
    }


    static Map<String, NamedMembers> getNamedMembers(@NotNull PsiClassWrapper psiClass, boolean staticMembers, boolean kotlin) {
        Builder builder = new Builder(psiClass, staticMembers, kotlin);
        builder.run();
        return builder.namedMembersMap;
    }


}
