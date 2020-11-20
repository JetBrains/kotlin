// FILE: Some.java

public class Some {
    public static final String HELLO = "HELLO";
}

// FILE: AnnotationInAnnotation.kt

annotation class Storage(val value: String)

annotation class State(val name: String, val storages: Array<Storage>)

@State(
    name = "1",
    storages = [Storage(value = Some.HELLO)]
)
class Test