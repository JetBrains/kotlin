// FIR_IDENTICAL
// JSPECIFY_STATE: strict
// WITH_STDLIB
// FIR_DUMP
// ISSUE: KT-59514

// FILE: lib/En.java

package lib;

public interface En<K extends Pr> extends Se<K> {}

// FILE: lib/Ke.java

package lib;

public interface Ke extends Ta {}

// FILE: lib/Pr.java

package lib;

public interface Pr extends Ke {}

// FILE: lib/R.java

package lib;

public interface R<T> {
    interface MR<T> extends R<T> {}
    interface F<S, T> extends R<T> {}
    interface FMR<S, T> extends F<S, T>, MR<T> {}
}

// FILE: lib/Se.java

package lib;

public interface Se<R extends Ta> {
}

// FILE: lib/Ta.java

package lib;

public interface Ta {
}

// FILE: lib/Il.java

package lib;

import org.jspecify.nullness.NullMarked;

@NullMarked
public abstract class Il<E> implements java.util.List<E> {
}

// FILE: lib/a/C.java

package lib.a;

import lib.Ta;
import lib.Se;
import lib.R;
import lib.Il;

class B {}

public final class C {
    public static <R extends Ta, S extends Se<R>> Fr<R, S> from(Class<S> c) {
        return null;
    }

    public interface WithL<T extends Ta, S extends Se<T>>
    extends CanWithB<T, S>, R.F<S, Il<S>> {}

    public interface CanWithL<T extends Ta, S extends Se<T>> {
        WithL<T, S> withL(int l);
    }

    public interface Fr<T extends Ta, S extends Se<T>>
    extends CanWithL<T, S>, R.F<S, Il<S>> {}

    public interface CanWithB<T extends Ta, S extends Se<T>> extends R.F<S, Il<S>> {
        WithB<T, S> withB(B b);
    }

    public interface WithB<T extends Ta, S extends Se<T>> extends R.F<S, Il<S>> {}

    static public class MyWithL<R extends Ta, S extends Se<R>> implements WithL<R, S> {
        public WithB<R, S> withB(B b) {
            return null;
        }
    }
}

// FILE: lib/a/Eq.kt

package lib.a

import lib.Se
import lib.Ta

data class A<R : Ta, S : Se<out R>>(val a: Class<out S>) {
    companion object {
        @JvmStatic
        fun <R : Ta, S : Se<R>> of(b: Class<out S>): A<R, S> = A(b)
    }
}

class Eq<R : Ta, S : Se<R>>(val a: A<R, S>) : C.Fr<R, S> {
    override fun withL(l: Int): C.WithL<R, S> {
        return C.MyWithL<R, S>()
    }
}

// FILE: repro/Repro.kt

package repro

import lib.R
import lib.a.C
import lib.Ke
import lib.Pr
import lib.En

abstract class Repro {
    abstract fun <T> f(r: R<T>): T
    abstract fun <P : Pr, E : En<P>> g(p: P): R.FMR<E, E>
    private fun isTableValid(c: Class<En<Pr>>, key: Pr, b: Boolean) {
        if (b) {
            f(g(key))
        } else {
            f(C.from(c).withL(1))
        }
    }
}
