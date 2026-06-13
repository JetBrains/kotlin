// FIR_IDENTICAL
// JSPECIFY_STATE: warn
// ISSUE: KT-86319

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

// FILE: com/example/ConcreteBuilderNullUnmarked.java

package com.example;

import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public class ConcreteBuilderNullUnmarked<B extends ConcreteBuilderNullUnmarked<B>> implements Builder<B> {
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

    public static ConcreteBuilderNullUnmarked<?> newBuilder() {
        return new ConcreteBuilderNullUnmarked<>();
    }
}

// FILE: main.kt

package com.example

fun test() {
    ConcreteBuilder.newBuilder()
        .name("test1")
        <!UNNECESSARY_SAFE_CALL!>?.<!>name("test2") // Making sure the return type is not flexible without @NullUnmarked
        ?.name("test3")
        ?.build()

    ConcreteBuilderNullUnmarked.newBuilder()
        .name("test1")
        ?.name("test2")
        ?.name("test3")
        ?.build()
}
