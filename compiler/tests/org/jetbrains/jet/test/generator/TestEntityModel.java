package org.jetbrains.jet.test.generator;

import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface TestEntityModel {
    String getName();

    @Nullable
    String getDataString();
}
