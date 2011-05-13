package org.jetbrains.jet.codegen;

import jet.JetObject;
import jet.typeinfo.TypeInfo;

import java.lang.reflect.Method;

/**
 * @author yole
 */
public class TypeInfoTest extends CodegenTestCase {
    @Override
    protected String getPrefix() {
        return "typeInfo";
    }

    public void testGetTypeInfo() throws Exception {
        loadFile();
        Method foo = generateFunction();
        JetObject jetObject = (JetObject) foo.invoke(null);
        TypeInfo<?> typeInfo = jetObject.getTypeInfo();
        assertNotNull(typeInfo);
    }

    public void testTypeOfOperator() throws Exception {
        loadFile();
        Method foo = generateFunction();
        TypeInfo typeInfo = (TypeInfo) foo.invoke(null);
        assertNotNull(typeInfo);
    }
}

