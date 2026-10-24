"""Generates a synthetic but typical Kotlin code base as one test data file with `// FILE:` sections.

usage: gen.py <files> <out.kt>
Each file N declares a package bench.pN with DTOs, an enum, a sealed hierarchy, a generic repository interface,
an abstract base service, a service with companion/JvmOverloads/suspend/vararg/JvmName/Deprecated members,
an object, a value class, nested/inner classes, an annotation class, top-level functions and properties.
"""
import sys

n_files = int(sys.argv[1]); out = sys.argv[2]

def file(i):
    p = f"bench.p{i}"
    prev = f"bench.p{i - 1}" if i > 0 else None
    imp = f"import {prev}.Dto{i - 1}\nimport {prev}.Status{i - 1}\n" if prev else ""
    parent_t = f"Dto{i - 1}?" if prev else f"Dto{i}?"
    collection_cls = f"""
class TagList{i}(private val tags: List<String>) : AbstractList<String>() {{
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}}
""" if i % 5 == 0 else ""
    delegation_cls = f"""
class LoggingRepo{i}(private val delegate: Repo{i}<Dto{i}, Long>) : Repo{i}<Dto{i}, Long> by delegate {{
    override fun save(entity: Dto{i}): Dto{i} = delegate.save(entity)
}}
""" if i % 4 == 0 else ""
    return f"""// FILE: p{i}/Model{i}.kt
@file:JvmName("Model{i}Kt")
package {p}

{imp}
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker{i}(val value: String, val priority: Int = 0)

@Marker{i}("dto", priority = {i})
data class Dto{i}(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: {parent_t} = null,
    var counter: Int = 0,
) {{
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto{i} = copy(name = newName)
}}

enum class Status{i}(val code: Int) {{
    ACTIVE(1),
    SUSPENDED(2) {{
        override fun label(): String = "suspended"
    }},
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {{
        fun byCode(code: Int): Status{i}? = entries.firstOrNull {{ it.code == code }}
    }}
}}

sealed interface Result{i}<out T> {{
    data class Ok<T>(val value: T) : Result{i}<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result{i}<Nothing>
    object Loading : Result{i}<Nothing>
}}

@JvmInline
value class Id{i}(val raw: Long)

interface Repo{i}<T : Any, ID : Comparable<ID>> {{
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}}

abstract class BaseService{i}<T : Any>(protected val repo: Repo{i}<T, Long>) {{
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = {{ true }}): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}}

// FILE: p{i}/Service{i}.kt
package {p}

{imp}
class Service{i}(
    repo: Repo{i}<Dto{i}, Long>,
    private val clock: () -> Long,
) : BaseService{i}<Dto{i}>(repo), Comparable<Service{i}> {{
    override fun validate(entity: Dto{i}): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto{i}>, filter: (Dto{i}) -> Boolean): List<Dto{i}> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto{i}> = emptyList()

    suspend fun refresh(force: Boolean): Result{i}<Dto{i}> = Result{i}.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto{i}) -> R?): List<Dto{i}> = emptyList()

    fun log(level: Int, vararg messages: String) {{}}

    fun findByTypedId(id: Id{i}): Dto{i}? = null

    fun status(dto: Dto{i}): Status{i} = Status{i}.ACTIVE

    fun copyInto(target: MutableList<in Dto{i}>, source: List<out Dto{i}>) {{}}

    fun subscribe(listener: suspend (Dto{i}) -> Unit): AutoCloseable = AutoCloseable {{}}

    @Deprecated("Use find")
    fun legacy(): Dto{i}? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker{i}("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto{i}>

    val stats: Map<String, Int> by lazy {{ emptyMap() }}

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service{i}"

    override fun compareTo(other: Service{i}): Int = 0

    inner class Session(val token: String) {{
        fun service(): Service{i} = this@Service{i}
    }}

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {{
        const val VERSION: Int = {i}

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo{i}<Dto{i}, Long>): Service{i} = Service{i}(repo) {{ 0L }}

        private val logger = Any()
    }}
}}

object Registry{i} {{
    private val items = mutableListOf<Any>()
    fun register(item: Any) {{ items += item }}
    val count: Int get() = items.size
    @JvmStatic fun reset() {{ items.clear() }}
}}
{collection_cls}{delegation_cls}
// FILE: p{i}/Utils{i}.kt
package {p}

const val MAX_ITEMS_{i}: Int = 1000

val defaultTags{i}: List<String> = listOf("a", "b")

var mutableSetting{i}: String? = null

fun Dto{i}.display(): String = name

val Dto{i}.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull{i}(): T? = getOrNull(1)

fun createDto{i}(id: Long, name: String = "dto", vararg tags: String): Dto{i} = Dto{i}(id, name, tags.toList())

inline fun <reified T> typeName{i}(): String = T::class.java.name

suspend fun loadAll{i}(service: Service{i}): List<Dto{i}> = emptyList()

internal fun internalUtil{i}(x: Int): Int = x + 1
"""

with open(out, 'w') as f:
    f.write("// WITH_STDLIB\n")
    for i in range(n_files):
        f.write(file(i))
