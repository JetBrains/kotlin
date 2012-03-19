/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Stepan Koltsov
 */
class JavaDescriptorResolverHelper {
    private JavaDescriptorResolverHelper() {
    }

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
        
        private boolean includeMember(PsiMemberWrapper member) {
            if (member.isStatic() != staticMembers) {
                return false;
            }

            if (!staticMembers && member.getPsiMember().getContainingClass() != psiClass.getPsiClass()) {
                return false;
            }
            
            if (member.isPrivate()) {
                return false;
            }

            return true;
        }
        
        private void processFields() {
            if (!kotlin) {
                for (PsiField field0 : psiClass.getPsiClass().getFields()) {
                    PsiFieldWrapper field = new PsiFieldWrapper(field0);
                    
                    if (!includeMember(field)) {
                        continue;
                    }

                    NamedMembers namedMembers = getNamedMembers(field.getName());

                    TypeSource type = new TypeSource("", field.getType(), field0);
                    namedMembers.addPropertyAccessor(new PropertyAccessorData(field, type, null));
                }
            }
        }

        private void processMethods() {
            
            for (PsiMethod method : psiClass.getPsiClass().getAllMethods()) {
                getNamedMembers(method.getName());
            }

            
            for (PsiMethod method0 : psiClass.getPsiClass().getMethods()) {
                PsiMethodWrapper method = new PsiMethodWrapper(method0);
                
                if (!includeMember(method)) {
                    continue;
                }

                // TODO: "is" prefix
                // TODO: remove getJavaClass
                if (method.getName().startsWith(JvmAbi.GETTER_PREFIX)) {

                    String propertyName = StringUtil.decapitalize(method.getName().substring(JvmAbi.GETTER_PREFIX.length()));
                    NamedMembers members = getNamedMembers(propertyName);

                    // TODO: some java properties too
                    if (method.getJetMethod().kind() == JvmStdlibNames.JET_METHOD_KIND_PROPERTY) {

                        int i = 0;

                        TypeSource receiverType;
                        if (i < method.getParameters().size() && method.getParameter(i).getJetValueParameter().receiver()) {
                            PsiParameterWrapper receiverParameter = method.getParameter(i);
                            receiverType = new TypeSource(receiverParameter.getJetValueParameter().type(), receiverParameter.getPsiParameter().getType(), receiverParameter.getPsiParameter());
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
                            throw new IllegalStateException("something is wrong with method " + method0);
                        }

                        // TODO: what if returnType == null?
                        TypeSource propertyType = new TypeSource(method.getJetMethod().propertyType(), method.getReturnType(), method.getPsiMethod());

                        members.addPropertyAccessor(new PropertyAccessorData(method, true, propertyType, receiverType));
                    } else if (!kotlin && false) {
                        if (method.getParameters().size() == 0) {
                            TypeSource propertyType = new TypeSource("", method.getReturnType(), method.getPsiMethod());
                            members.addPropertyAccessor(new PropertyAccessorData(method, true, propertyType, null));
                        }
                    }

                } else if (method.getName().startsWith(JvmAbi.SETTER_PREFIX)) {

                    String propertyName = StringUtil.decapitalize(method.getName().substring(JvmAbi.SETTER_PREFIX.length()));
                    NamedMembers members = getNamedMembers(propertyName);

                    if (method.getJetMethod().kind() == JvmStdlibNames.JET_METHOD_KIND_PROPERTY) {
                        if (method.getParameters().size() == 0) {
                            // TODO: report error properly
                            throw new IllegalStateException();
                        }

                        int i = 0;

                        TypeSource receiverType = null;
                        PsiParameterWrapper p1 = method.getParameter(0);
                        if (p1.getJetValueParameter().receiver()) {
                            receiverType = new TypeSource(p1.getJetValueParameter().type(), p1.getPsiParameter().getType(), p1.getPsiParameter());
                            ++i;
                        }

                        while (i < method.getParameters().size() && method.getParameter(i).getJetTypeParameter().isDefined()) {
                            ++i;
                        }

                        if (i + 1 != method.getParameters().size()) {
                            throw new IllegalStateException();
                        }

                        PsiParameterWrapper propertyTypeParameter = method.getParameter(i);
                        TypeSource propertyType = new TypeSource(method.getJetMethod().propertyType(), propertyTypeParameter.getPsiParameter().getType(), propertyTypeParameter.getPsiParameter());

                        members.addPropertyAccessor(new PropertyAccessorData(method, false, propertyType, receiverType));
                    } else if (!kotlin && false) {
                        if (method.getParameters().size() == 1) {
                            PsiParameter psiParameter = method.getParameters().get(0).getPsiParameter();
                            TypeSource propertyType = new TypeSource("", psiParameter.getType(), psiParameter);
                            members.addPropertyAccessor(new PropertyAccessorData(method, false, propertyType, null));
                        }
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
