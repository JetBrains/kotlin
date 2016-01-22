import java.util.concurrent.atomic.*

fun f() {
    AtomicReferenceFieldUpdater.newUpdater<String, Int>(<caret>)
}

// ELEMENT: String