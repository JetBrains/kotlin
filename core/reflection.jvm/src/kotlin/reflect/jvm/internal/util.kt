/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal

import java.lang.reflect.Method

private fun String.capitalizeWithJavaBeanConvention(): String {
    if (length() > 1 && Character.isUpperCase(this[1])) return this
    return Character.toUpperCase(this[0]) + substring(1, length())
}

private fun getterName(propertyName: String): String = "get" + propertyName.capitalizeWithJavaBeanConvention()
private fun setterName(propertyName: String): String = "set" + propertyName.capitalizeWithJavaBeanConvention()


private fun Class<*>.getMaybeDeclaredMethod(name: String, vararg parameterTypes: Class<*>): Method {
    try {
        return getMethod(name, *parameterTypes)
    }
    catch (e: NoSuchMethodException) {
        // This is needed to support private methods
        return getDeclaredMethod(name, *parameterTypes)
    }
}
