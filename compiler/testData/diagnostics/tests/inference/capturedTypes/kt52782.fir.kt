// SKIP_TXT
// FILE: Foo.java
public abstract class Foo<K extends Bar<? extends Foo<K>>> {
    abstract String getTest();
}

// FILE: Bar.java
public abstract class Bar<T extends Foo<? extends Bar<T>>> {}

// FILE: main.kt

fun <F : Bar<out Foo<F>>> Foo<F>.bar() {}

fun box(foo: Foo<*>) {
    // How inference for the `foo.bar()` call works
    //
    // X = Captured(*)
    // Foo<X> <: Foo<Fv> => X = Fv => NewBound(X <: Fv), NewBound(Fv <: X)
    //
    // Incorporation 1:
    // X <: Fv && Fv <: Bar<out Foo<Fv>> => X <: Bar<out Foo<Fv>>
    // X has one supertype – Bar<out Foo<X>> ~ Bar<Y> where Y = Captured(out Foo<X>)
    // X <: Bar<out Foo<Fv>> => Bar<Y> <: Bar<out Foo<Fv>> =>
    // Y <: Foo<Fv>
    // Y has two supertypes --> (Foo<out Bar<Y>>, Foo<X>)
    // Let Z be a captured type in Foo<out Bar<Y>>
    // Foo<Z> <: Foo<Fv> => Z = Fv =>
    // But since Z is captured from subtyping we run approximation over it resulting to
    // Z ~ Bar<out Foo<*>>
    // NewBound(Bar<out Foo<*>> <: Fv)
    //
    // But since Fv <: X, Bar<out Foo<*>> should be a subtype of X, but it is not :(
    //
    // Note the problematic part is when Y has two supertypes
    // And we just check only first one one the second one would work
    // In K2, it's covered by fork-points, see https://youtrack.jetbrains.com/issue/KT-50401 and org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector.TypeCheckerStateForConstraintInjector.runForkingPoint


    // Without approximation (just theoretically what would happen if we didn't approximated FROM_SUBTYPING captured types).
    // Actually, it's not really relevant but just in case someone need it.
    //
    // X = Captured(*)
    // Foo<X> <: Foo<Fv> => X = Fv => NewBound(X <: Fv), NewBound(Fv <: X)
    //
    // Incorporation 1:
    // X <: Fv && Fv <: Bar<out Foo<Fv>> => X <: Bar<out Foo<Fv>>
    // X has one supertype – Bar<out Foo<X>> ~ Bar<Y> where Y = Captured(out Foo<X>)
    // X <: Bar<out Foo<Fv>> => Bar<Y> <: Bar<out Foo<Fv>> =>
    // Y <: Foo<Fv>
    // Y has two supertypes --> (Foo<out Bar<Y>>, Foo<X>)
    // Let Z be a captured type in Foo<out Bar<Y>>
    // Foo<Z> <: Foo<Fv> => Z = Fv => NewBound(Z <: Fv)
    //
    // Incorporation 2:
    // Z <: Fv && Fv <: Bar<out Foo<Fv>> => Z <: Bar<out Foo<Fv>>
    // Z has two supertypes --> (Bar<out Foo<Z>>, Bar<Y>)
    // ... went into recursion

    foo.bar()

    foo.test
}
