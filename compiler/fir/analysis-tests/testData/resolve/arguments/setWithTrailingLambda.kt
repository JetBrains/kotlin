// ISSUE: KT-56714

fun test(m: MyMap<EditorData, Any>) {
    m.set(SomeKey) { _, _ -> }
    m[SomeKey] = { _, _ -> }
}

data class EditorData(val meta: MyMap<EditorData, Any>)

interface MyMap<Domain, V : Any> {
    operator fun <T : V> set(k: Key<T, Domain>, v: T)
}

interface Key<V : Any, in Domain>

interface EditorDataKey<T : Any> : Key<T, EditorData>

object SomeKey : EditorDataKey<(String, bar: Any) -> Unit>
