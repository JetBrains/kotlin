// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

class A {
    open class B
}

class Outer {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    class A {
        open class B

        class C : A.B() {
            // 1. type resolver works the following way:
            //   - iterates scopes
            //   - finds symbol for `A` : `Outer.A`
            //   - resolves whold `A.B` chain starting from this `Outer.A` symbol
            //   - only checks that resulting `Outer.A.B` is hidden (it is not)
            //   - result: `Outer.A.B`
            // 2. body resolver:
            //   - first resolves `A`
            //     - finds `Outer.A` symbol
            //     - it's hidden
            //     - finds `<root>.A` symbol
            //   - then resolves `B` with `<root>.A` receiver
            //   - result: `<root>.A.B`
            override fun equals(@EqualityBound(<!AMBIGUOUSLY_RESOLVED_EQUALITY_BOUND_ARGUMENT!>A.B<!>::class) other: Any?): Boolean {
                return super.equals(other)
            }
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nestedClass, nullableType, operator,
override, stringLiteral, superExpression */
