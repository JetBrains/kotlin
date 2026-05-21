// FIR_IDENTICAL
// JSPECIFY_STATE: warn

// FILE: com/example/package-info.java

@NullMarked
package com.example;

import org.jspecify.annotations.NullMarked;

// FILE: com/example/Builder.java

package com.example;

public interface Builder<B extends Builder<B>> {
    B name(String name);
    String build();
}

// FILE: com/example/ConcreteBuilder.java

package com.example;

import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public class ConcreteBuilder<B extends ConcreteBuilder<B>> implements Builder<B> {
    private String name;

    @Override
    public B name(String name) {
        this.name = name;
        return (B) this;
    }

    @Override
    public String build() {
        return name;
    }

    public static ConcreteBuilder<?> newBuilder() {
        return new ConcreteBuilder<>();
    }
}

// FILE: main.kt

package com.example

fun test(): String {
    return ConcreteBuilder.newBuilder()
        .name("test")
        .build()
}
