// FILE: Named.java

public interface Named {
    String getName();
}

// FILE: Psi.java

public interface Psi<D> {

}

// FILE: Member.java

public interface Member<D> extends Psi<D> {
}

// FILE: TypeParametersOwner.java

import java.util.List;

public interface TypeParametersOwner {
    public List<String> getTypeParameters();
}

// FILE: Klass.java

public interface Klass extends TypeParametersOwner {
}

// FILE: Test.kt

fun List<String>.single(): String = ""
fun List<Int>.single(): Int = 2

fun listOf(): List<String> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

public open class LightMember<D> : Member<D>, Light() {
    override fun getName(): String = "Light"
}

public <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class LightClassWrapper<!> : Light(), Klass {
    fun test() = typeParameters.single()
}

public abstract class Light : Field, TypeParametersOwner {
    fun <!VIRTUAL_MEMBER_HIDDEN!>getTypeParameters<!>() = listOf()
}

public interface Field : Named

public class LightField<D> : LightMember<Any>(), Field {
    fun test(other: Any?) {
        if (other is LightField<*>) {
            other.name
        }
    }
}

