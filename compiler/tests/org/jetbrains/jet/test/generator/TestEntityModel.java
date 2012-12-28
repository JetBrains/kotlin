package org.jetbrains.jet.test.generator;

import org.jetbrains.annotations.Nullable;

public interface TestEntityModel {
    String getName();

    @Nullable
    String getDataString();
}
