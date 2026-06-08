// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB
// WITH_COROUTINES
// LANGUAGE: +MultiPlatformProjects

// MODULE: lib
// FILE: Lib.kt
package lib

abstract class Base

interface I<T>
// T -> {Int: [A], String: [B], I<T>: [C<T>]} | the concrete types of I<T> (for I<T>: [C<T>]) depend on the concrete types C<T> takes on for T
// T -> {Int: [A], String: [B], I<D<Int>>: [C<D<Int>>], I<D<F>>: [C<D<F>>]}

// MODULE: main(lib)
// FILE: Main.kt
package main

import lib.*

class Derived : Base(), I<Int>

class A : I<Int>
// <empty>

class B : I<String>
// <empty>

open class C<T> : I<I<T>>
// T -> {D<T>: [D<T>]} | the concrete types of D<T> (key) (for D<T>: [D<T>]) depend on the concrete types D<T> (value) takes on for T
// T -> {D<Int>: [D<Int>], D<F>: [D<F>]}

open class D<T> : C<D<T>>()
// T -> {Int: [E], F: [F, G]}

class E : D<Int>()
// <empty>

class F : D<F>()
// <empty>

class G : D<F>()
// <empty>

// FILE: Test.kt
import kotlin.coroutines.*

interface CoroutineTracerShim {
  companion object {
    var coroutineTracer: CoroutineTracerShim = object : CoroutineTracerShim {
      override fun rootTrace() = EmptyCoroutineContext
    }

    fun foo() {
        class Local : CoroutineTracerShim {
            override fun rootTrace() = EmptyCoroutineContext
        }
    }
  }

  fun rootTrace(): CoroutineContext
}

// MODULE: common
// FILE: Common.kt
interface MyDataItem {
    val id: String
    val text1: String
    val text2: String
}

// MODULE: platform(common)
// FILE: Platform.kt
fun createItemVM() = object {
    inner class MyItemVM(data: MyDataItem) : MyDataItem by data {
        val isSelected = false
    }
}

// MODULE: javainterop
// FILE: Callback.java
import java.util.*;

public interface Callback {
    interface UncaughtExceptionHandler {
        /** Method invoked when the given callback throws an uncaught
         * exception.<p>
         * Any exception thrown by this method will be ignored.
         */
        void uncaughtException(Callback c, Throwable e);
    }
    /** You must use this method name if your callback interface has multiple
        public methods.  Typically a callback will have only one such
        method, in which case any method name may be used, with the exception
        of those in {@link #FORBIDDEN_NAMES}.
    */
    String METHOD_NAME = "callback";

    /** These method names may not be used for a callback method. */
    List<String> FORBIDDEN_NAMES = Collections.unmodifiableList(
            Arrays.asList("hashCode", "equals", "toString"));
}

// FILE: Graphics.java
public abstract class Graphics {

}

// FILE: JComponent.java
public abstract class JComponent {

}

// FILE: ComponentUI.java
public abstract class ComponentUI {
    public ComponentUI() {

    }

    public void installUI(JComponent c) {
    }

    public void uninstallUI(JComponent c) {
    }

    public void paint(Graphics g, JComponent c) {
    }
}

// FILE: ScrollBarUI.java
public abstract class ScrollBarUI extends ComponentUI {
    protected ScrollBarUI() {
    }
}

// FILE: DefaultScrollBarUI.kt
open class DefaultScrollBarUI @JvmOverloads internal constructor(
    private val thickness: Int = 10,
    private val thicknessMax: Int = 14,
    private val thicknessMin: Int = 10,
) : ScrollBarUI() {
    override fun installUI(c: JComponent) {
    }

    override fun uninstallUI(c: JComponent) {
    }

    override fun paint(g: Graphics, c: JComponent) {
    }
}

// FILE: MacScrollBarUI.kt
private abstract class MacScrollbarNative<T> : Callback, Runnable, () -> T? {
    private var value: T? = null

    override fun invoke() = value

    override fun run() = Unit
}

internal enum class MacScrollbarStyle {
    Legacy, Overlay;
}

internal open class MacScrollBarUI : DefaultScrollBarUI {
    constructor(thickness: Int, thicknessMax: Int, thicknessMin: Int) : super(
        thickness = thickness,
        thicknessMax = thicknessMax,
        thicknessMin = thicknessMin,
    )

    constructor() : super(thickness = 14, thicknessMax = 14, thicknessMin = 11)

    companion object {
        private val CURRENT_STYLE = object : MacScrollbarNative<MacScrollbarStyle>() {
            override fun run() = Unit

            override fun invoke(): MacScrollbarStyle = MacScrollbarStyle.Overlay
        }
    }

    override fun installUI(c: JComponent) {
        super.installUI(c)
    }

    override fun uninstallUI(c: JComponent) {
        super.uninstallUI(c)
    }

    override fun paint(g: Graphics, c: JComponent) {
        super.paint(g, c)
    }
}


/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration, companionObject,
functionDeclaration, functionalType, inheritanceDelegation, inner, interfaceDeclaration, lambdaLiteral, localClass,
localProperty, nullableType, objectDeclaration, override, primaryConstructor, propertyDeclaration, safeCall, suspend,
thisExpression, typeParameter */
