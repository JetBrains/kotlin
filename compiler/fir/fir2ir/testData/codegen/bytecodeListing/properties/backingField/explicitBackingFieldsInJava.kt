// MODULE: ModuleA
// FILE: AI.kt

public interface AI {
    val number: Number
}

// FILE: AC.kt

public class AC : AI {
    final override val number: Number
        field = 4
}
