/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.wrapper;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.TypeSource;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*
* Data from PSI related to one property used to resolve this property.
*/
public final class PropertyPsiData {

    @NotNull
    public static Collection<PropertyPsiData> assemblePropertyPsiDataFromElements(@NotNull List<PropertyPsiDataElement> elements) {
        Map<String, PropertyPsiData> map = new HashMap<String, PropertyPsiData>();
        for (PropertyPsiDataElement element : elements) {
            String key = propertyKeyForGrouping(element);

            PropertyPsiData value = map.get(key);
            if (value == null) {
                value = new PropertyPsiData();
                map.put(key, value);
            }

            if (element.isGetter()) {
                checkDuplicatePropertyComponent(element, "getter", value.getter);
                value.getter = element;
            }
            else if (element.isSetter()) {
                checkDuplicatePropertyComponent(element, "setter", value.setter);
                value.setter = element;
            }
            else if (element.isField()) {
                checkDuplicatePropertyComponent(element, "field", value.field);
                value.field = element;
            }
            else {
                throw new IllegalStateException();
            }
        }

        return map.values();
    }

    private static void checkDuplicatePropertyComponent(
            @NotNull PropertyPsiDataElement checked, @NotNull String componentTypeName, @Nullable PropertyPsiDataElement existent
    ) {
        if (existent != null) {
            PsiClass checkedElementClass = checked.getMember().getPsiMember().getContainingClass();
            PsiClass existentElementClass = existent.getMember().getPsiMember().getContainingClass();

            throw new IllegalStateException(
                    String.format("Psi element '%s' in class '%s' overwrites '%s' in class '%s' while generating %s component for property",
                                  checked.getMember().getPsiMember(),
                                  checkedElementClass != null ? checkedElementClass.getQualifiedName() : "<no-class>",
                                  existent.getMember().getPsiMember(),
                                  existentElementClass != null ? existentElementClass.getQualifiedName() : "<no-class>",
                                  componentTypeName));
        }
    }

    @NotNull
    private static String propertyKeyForGrouping(@NotNull PropertyPsiDataElement propertyAccessor) {
        String type = key(propertyAccessor.getType());
        String receiverType = key(propertyAccessor.getReceiverType());
        return Pair.create(type, receiverType).toString();
    }

    @NotNull
    private static String key(@Nullable TypeSource typeSource) {
        if (typeSource == null) {
            return "";
        }
        else if (typeSource.getTypeString().length() > 0) {
            return typeSource.getTypeString();
        }
        else {
            return typeSource.getPsiType().getPresentableText();
        }
    }

    @Nullable
    private PropertyPsiDataElement getter = null;
    @Nullable
    private PropertyPsiDataElement setter = null;
    @Nullable
    private PropertyPsiDataElement field = null;
    @Nullable
    private Collection<PropertyPsiDataElement> elements = null;

    @Nullable
    public PropertyPsiDataElement getGetter() {
        return getter;
    }

    @Nullable
    public PropertyPsiDataElement getSetter() {
        return setter;
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    private Collection<PropertyPsiDataElement> getElements() {
        if (elements == null) {
            elements = Lists.newArrayList();
            if (getter != null) {
                elements.add(getter);
            }
            if (setter != null) {
                elements.add(setter);
            }
            if (field != null) {
                elements.add(field);
            }
            assert !elements.isEmpty();
        }
        return elements;
    }

    public boolean isExtension() {
        boolean isExtension = getCharacteristicMember().isExtension();
        for (PropertyPsiDataElement element : getElements()) {
            assert (element.isExtension() == isExtension);
        }
        return isExtension;
    }

    public boolean isStatic() {
        boolean isStatic = getCharacteristicMember().getMember().isStatic();
        for (PropertyPsiDataElement element : getElements()) {
            assert (element.getMember().isStatic() == isStatic);
        }
        return isStatic;
    }

    @NotNull
    public PropertyPsiDataElement getCharacteristicMember() {
        if (getter != null) {
            return getter;
        }
        if (field != null) {
            return field;
        }
        if (setter != null) {
            return setter;
        }
        throw new IllegalStateException();
    }

    @NotNull
    public PsiMember getCharacteristicPsi() {
        return getCharacteristicMember().getMember().getPsiMember();
    }

    public boolean isVar() {
        if (getter == null && setter == null) {
            assert field != null;
            return !field.getMember().isFinal();
        }
        return setter != null;
    }

    public boolean isStaticFinalField() {
        if (getter != null || setter != null) {
            return false;
        }
        assert field != null;
        return field.getMember().isFinal() && field.getMember().isStatic();
    }

    public boolean isPropertyForNamedObject() {
        return field != null && JvmAbi.INSTANCE_FIELD.equals(field.getMember().getName());
    }
}
