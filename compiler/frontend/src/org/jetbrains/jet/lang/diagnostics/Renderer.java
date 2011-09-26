package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface Renderer {
    Renderer TO_STRING = new Renderer() {
        @NotNull
        @Override
        public String render(@Nullable Object object) {
            return object == null ? "null" : object.toString();
        }

        @Override
        public String toString() {
            return "TO_STRING";
        }
    };

    @NotNull
    String render(@Nullable Object object);
}
