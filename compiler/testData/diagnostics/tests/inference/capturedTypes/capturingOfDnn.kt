// FIR_IDENTICAL

// FILE: Inlay.java
public interface Inlay<T extends EditorCustomElementRenderer> {
    @org.jetbrains.annotations.NotNull T getRenderer();
}

// FILE: test.kt
interface EditorCustomElementRenderer
interface PresentationContainerRenderer<Constraints : Any> : EditorCustomElementRenderer

fun test(inlay: Inlay<out PresentationContainerRenderer<*>>) {
    inlay.renderer.addOrUpdate()
}

fun <T : Any> PresentationContainerRenderer<T>.addOrUpdate() {}