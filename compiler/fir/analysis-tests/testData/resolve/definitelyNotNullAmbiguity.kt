// FILE: KtVisitor.java

public class KtVisitor<R, D> {}

// FILE: A.java

public interface A {
    public <R, D> R accept(@org.jetbrains.annotations.NotNull KtVisitor<R, D> visitor, D data)
}

// FILE: B.kt

interface B : A {
    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R
}

// FILE: main.kt

fun test(visitor: KtVisitor<String, Unit>, element: B) {
    element.accept(visitor, Unit)
}
