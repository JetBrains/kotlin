/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package headerWarnings

// Note: the test parses the generated header with -Werror to detect warnings.

class TestIncompatiblePropertyTypeWarning {
    class Generic<T>(val value: T)

    interface InterfaceWithGenericProperty<T> {
        val p: Generic<T>
    }

    class ClassOverridingInterfaceWithGenericProperty(override val p: Generic<String>) : InterfaceWithGenericProperty<String>
}
