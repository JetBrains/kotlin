// FILE: JavaInterface.java
import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface JavaInterface<T> {
    public void doSmth(@NotNull T x);
}

// FILE: KotlinInterface.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno1

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs1(val x: String)

@Target(AnnotationTarget.TYPE)
annotation class Anno2

interface KotlinInterface<T1> : JavaInterface<T1> {
    override fun doSmth(x: <expr> @Anno1 @AnnoWithArgs1("") @Anno2 T1 & @Anno1 </expr>)
}
