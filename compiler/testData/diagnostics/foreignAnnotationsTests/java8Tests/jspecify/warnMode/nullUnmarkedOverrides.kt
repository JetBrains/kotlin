// FIR_IDENTICAL
// JSPECIFY_STATE: strict

// FILE: com/example/package-info.java

@NullMarked
package com.example;

import org.jspecify.annotations.NullMarked;

// FILE: com/example/BoundType.java

package com.example;

public interface BoundType<E extends CharSequence> {
}

// FILE: com/example/SelfType.java

package com.example;

public interface SelfType<S extends SelfType<S>> {
}

// FILE: com/example/MyClassWithNullUnmarked.java

package com.example;


import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public class MyClassWithNullUnmarked {
    public void foo(BoundType<?> x) {}
    public void bar(SelfType<?> x) {}
}

// FILE: main.kt

package com.example

class StarProjectionsOverrides : MyClassWithNullUnmarked() {
    override fun foo(x: BoundType<*>) {}
    override fun bar(x: SelfType<*>) {}
}

class TypeProjectionsOverrides : MyClassWithNullUnmarked() {
    override fun foo(x: BoundType<out CharSequence>) {}
    override fun bar(x: SelfType<out SelfType<*>>) {}
}

class TypeProjectionsOverridesNullable : MyClassWithNullUnmarked() {
    override fun foo(x: BoundType<<!UPPER_BOUND_VIOLATED!>out CharSequence?<!>>) {}
    override fun bar(x: SelfType<<!UPPER_BOUND_VIOLATED!>out SelfType<*>?<!>>) {}
}

class AnyProjectionsOverrides : MyClassWithNullUnmarked() {
    override fun foo(x: BoundType<<!UPPER_BOUND_VIOLATED!>out Any<!>>) {}
    override fun bar(x: SelfType<<!UPPER_BOUND_VIOLATED!>out Any<!>>) {}
}

