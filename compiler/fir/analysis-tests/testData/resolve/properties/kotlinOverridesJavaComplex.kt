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

// FILE: Test.kt

public interface Light

public class LightMember<D> : Light, Member<D> {
    override fun getName(): String = "Light"
}

public interface Field : Named

public class LightField<D> : LightMember<Any>(), Field {
    fun test(other: Any?) {
        if (other is LightField<*>) {
            other.name
        }
    }
}

