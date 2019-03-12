/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container.tests;

public class JavaTestComponents {
    public final Iterable<TestClientComponentInterface> components;
    public final Iterable<TestGenericComponent<String>> genericComponents;

    public JavaTestComponents(Iterable<TestClientComponentInterface> components,
            Iterable<TestGenericComponent<String>> genericComponents) {
        this.components = components;
        this.genericComponents = genericComponents;
    }
}
