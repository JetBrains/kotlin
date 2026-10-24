// WITH_STDLIB
// FILE: p0/Model0.kt
@file:JvmName("Model0Kt")
package bench.p0


@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker0(val value: String, val priority: Int = 0)

@Marker0("dto", priority = 0)
data class Dto0(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto0? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto0 = copy(name = newName)
}

enum class Status0(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status0? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result0<out T> {
    data class Ok<T>(val value: T) : Result0<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result0<Nothing>
    object Loading : Result0<Nothing>
}

@JvmInline
value class Id0(val raw: Long)

interface Repo0<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService0<T : Any>(protected val repo: Repo0<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p0/Service0.kt
package bench.p0


class Service0(
    repo: Repo0<Dto0, Long>,
    private val clock: () -> Long,
) : BaseService0<Dto0>(repo), Comparable<Service0> {
    override fun validate(entity: Dto0): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto0>, filter: (Dto0) -> Boolean): List<Dto0> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto0> = emptyList()

    suspend fun refresh(force: Boolean): Result0<Dto0> = Result0.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto0) -> R?): List<Dto0> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id0): Dto0? = null

    fun status(dto: Dto0): Status0 = Status0.ACTIVE

    fun copyInto(target: MutableList<in Dto0>, source: List<out Dto0>) {}

    fun subscribe(listener: suspend (Dto0) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto0? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker0("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto0>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service0"

    override fun compareTo(other: Service0): Int = 0

    inner class Session(val token: String) {
        fun service(): Service0 = this@Service0
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 0

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo0<Dto0, Long>): Service0 = Service0(repo) { 0L }

        private val logger = Any()
    }
}

object Registry0 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class TagList0(private val tags: List<String>) : AbstractList<String>() {
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}

class LoggingRepo0(private val delegate: Repo0<Dto0, Long>) : Repo0<Dto0, Long> by delegate {
    override fun save(entity: Dto0): Dto0 = delegate.save(entity)
}

// FILE: p0/Utils0.kt
package bench.p0

const val MAX_ITEMS_0: Int = 1000

val defaultTags0: List<String> = listOf("a", "b")

var mutableSetting0: String? = null

fun Dto0.display(): String = name

val Dto0.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull0(): T? = getOrNull(1)

fun createDto0(id: Long, name: String = "dto", vararg tags: String): Dto0 = Dto0(id, name, tags.toList())

inline fun <reified T> typeName0(): String = T::class.java.name

suspend fun loadAll0(service: Service0): List<Dto0> = emptyList()

internal fun internalUtil0(x: Int): Int = x + 1
// FILE: p1/Model1.kt
@file:JvmName("Model1Kt")
package bench.p1

import bench.p0.Dto0
import bench.p0.Status0

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker1(val value: String, val priority: Int = 0)

@Marker1("dto", priority = 1)
data class Dto1(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto0? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto1 = copy(name = newName)
}

enum class Status1(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status1? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result1<out T> {
    data class Ok<T>(val value: T) : Result1<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result1<Nothing>
    object Loading : Result1<Nothing>
}

@JvmInline
value class Id1(val raw: Long)

interface Repo1<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService1<T : Any>(protected val repo: Repo1<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p1/Service1.kt
package bench.p1

import bench.p0.Dto0
import bench.p0.Status0

class Service1(
    repo: Repo1<Dto1, Long>,
    private val clock: () -> Long,
) : BaseService1<Dto1>(repo), Comparable<Service1> {
    override fun validate(entity: Dto1): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto1>, filter: (Dto1) -> Boolean): List<Dto1> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto1> = emptyList()

    suspend fun refresh(force: Boolean): Result1<Dto1> = Result1.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto1) -> R?): List<Dto1> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id1): Dto1? = null

    fun status(dto: Dto1): Status1 = Status1.ACTIVE

    fun copyInto(target: MutableList<in Dto1>, source: List<out Dto1>) {}

    fun subscribe(listener: suspend (Dto1) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto1? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker1("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto1>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service1"

    override fun compareTo(other: Service1): Int = 0

    inner class Session(val token: String) {
        fun service(): Service1 = this@Service1
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 1

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo1<Dto1, Long>): Service1 = Service1(repo) { 0L }

        private val logger = Any()
    }
}

object Registry1 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p1/Utils1.kt
package bench.p1

const val MAX_ITEMS_1: Int = 1000

val defaultTags1: List<String> = listOf("a", "b")

var mutableSetting1: String? = null

fun Dto1.display(): String = name

val Dto1.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull1(): T? = getOrNull(1)

fun createDto1(id: Long, name: String = "dto", vararg tags: String): Dto1 = Dto1(id, name, tags.toList())

inline fun <reified T> typeName1(): String = T::class.java.name

suspend fun loadAll1(service: Service1): List<Dto1> = emptyList()

internal fun internalUtil1(x: Int): Int = x + 1
// FILE: p2/Model2.kt
@file:JvmName("Model2Kt")
package bench.p2

import bench.p1.Dto1
import bench.p1.Status1

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker2(val value: String, val priority: Int = 0)

@Marker2("dto", priority = 2)
data class Dto2(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto1? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto2 = copy(name = newName)
}

enum class Status2(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status2? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result2<out T> {
    data class Ok<T>(val value: T) : Result2<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result2<Nothing>
    object Loading : Result2<Nothing>
}

@JvmInline
value class Id2(val raw: Long)

interface Repo2<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService2<T : Any>(protected val repo: Repo2<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p2/Service2.kt
package bench.p2

import bench.p1.Dto1
import bench.p1.Status1

class Service2(
    repo: Repo2<Dto2, Long>,
    private val clock: () -> Long,
) : BaseService2<Dto2>(repo), Comparable<Service2> {
    override fun validate(entity: Dto2): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto2>, filter: (Dto2) -> Boolean): List<Dto2> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto2> = emptyList()

    suspend fun refresh(force: Boolean): Result2<Dto2> = Result2.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto2) -> R?): List<Dto2> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id2): Dto2? = null

    fun status(dto: Dto2): Status2 = Status2.ACTIVE

    fun copyInto(target: MutableList<in Dto2>, source: List<out Dto2>) {}

    fun subscribe(listener: suspend (Dto2) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto2? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker2("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto2>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service2"

    override fun compareTo(other: Service2): Int = 0

    inner class Session(val token: String) {
        fun service(): Service2 = this@Service2
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 2

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo2<Dto2, Long>): Service2 = Service2(repo) { 0L }

        private val logger = Any()
    }
}

object Registry2 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p2/Utils2.kt
package bench.p2

const val MAX_ITEMS_2: Int = 1000

val defaultTags2: List<String> = listOf("a", "b")

var mutableSetting2: String? = null

fun Dto2.display(): String = name

val Dto2.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull2(): T? = getOrNull(1)

fun createDto2(id: Long, name: String = "dto", vararg tags: String): Dto2 = Dto2(id, name, tags.toList())

inline fun <reified T> typeName2(): String = T::class.java.name

suspend fun loadAll2(service: Service2): List<Dto2> = emptyList()

internal fun internalUtil2(x: Int): Int = x + 1
// FILE: p3/Model3.kt
@file:JvmName("Model3Kt")
package bench.p3

import bench.p2.Dto2
import bench.p2.Status2

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker3(val value: String, val priority: Int = 0)

@Marker3("dto", priority = 3)
data class Dto3(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto2? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto3 = copy(name = newName)
}

enum class Status3(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status3? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result3<out T> {
    data class Ok<T>(val value: T) : Result3<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result3<Nothing>
    object Loading : Result3<Nothing>
}

@JvmInline
value class Id3(val raw: Long)

interface Repo3<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService3<T : Any>(protected val repo: Repo3<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p3/Service3.kt
package bench.p3

import bench.p2.Dto2
import bench.p2.Status2

class Service3(
    repo: Repo3<Dto3, Long>,
    private val clock: () -> Long,
) : BaseService3<Dto3>(repo), Comparable<Service3> {
    override fun validate(entity: Dto3): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto3>, filter: (Dto3) -> Boolean): List<Dto3> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto3> = emptyList()

    suspend fun refresh(force: Boolean): Result3<Dto3> = Result3.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto3) -> R?): List<Dto3> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id3): Dto3? = null

    fun status(dto: Dto3): Status3 = Status3.ACTIVE

    fun copyInto(target: MutableList<in Dto3>, source: List<out Dto3>) {}

    fun subscribe(listener: suspend (Dto3) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto3? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker3("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto3>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service3"

    override fun compareTo(other: Service3): Int = 0

    inner class Session(val token: String) {
        fun service(): Service3 = this@Service3
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 3

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo3<Dto3, Long>): Service3 = Service3(repo) { 0L }

        private val logger = Any()
    }
}

object Registry3 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p3/Utils3.kt
package bench.p3

const val MAX_ITEMS_3: Int = 1000

val defaultTags3: List<String> = listOf("a", "b")

var mutableSetting3: String? = null

fun Dto3.display(): String = name

val Dto3.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull3(): T? = getOrNull(1)

fun createDto3(id: Long, name: String = "dto", vararg tags: String): Dto3 = Dto3(id, name, tags.toList())

inline fun <reified T> typeName3(): String = T::class.java.name

suspend fun loadAll3(service: Service3): List<Dto3> = emptyList()

internal fun internalUtil3(x: Int): Int = x + 1
// FILE: p4/Model4.kt
@file:JvmName("Model4Kt")
package bench.p4

import bench.p3.Dto3
import bench.p3.Status3

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker4(val value: String, val priority: Int = 0)

@Marker4("dto", priority = 4)
data class Dto4(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto3? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto4 = copy(name = newName)
}

enum class Status4(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status4? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result4<out T> {
    data class Ok<T>(val value: T) : Result4<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result4<Nothing>
    object Loading : Result4<Nothing>
}

@JvmInline
value class Id4(val raw: Long)

interface Repo4<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService4<T : Any>(protected val repo: Repo4<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p4/Service4.kt
package bench.p4

import bench.p3.Dto3
import bench.p3.Status3

class Service4(
    repo: Repo4<Dto4, Long>,
    private val clock: () -> Long,
) : BaseService4<Dto4>(repo), Comparable<Service4> {
    override fun validate(entity: Dto4): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto4>, filter: (Dto4) -> Boolean): List<Dto4> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto4> = emptyList()

    suspend fun refresh(force: Boolean): Result4<Dto4> = Result4.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto4) -> R?): List<Dto4> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id4): Dto4? = null

    fun status(dto: Dto4): Status4 = Status4.ACTIVE

    fun copyInto(target: MutableList<in Dto4>, source: List<out Dto4>) {}

    fun subscribe(listener: suspend (Dto4) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto4? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker4("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto4>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service4"

    override fun compareTo(other: Service4): Int = 0

    inner class Session(val token: String) {
        fun service(): Service4 = this@Service4
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 4

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo4<Dto4, Long>): Service4 = Service4(repo) { 0L }

        private val logger = Any()
    }
}

object Registry4 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class LoggingRepo4(private val delegate: Repo4<Dto4, Long>) : Repo4<Dto4, Long> by delegate {
    override fun save(entity: Dto4): Dto4 = delegate.save(entity)
}

// FILE: p4/Utils4.kt
package bench.p4

const val MAX_ITEMS_4: Int = 1000

val defaultTags4: List<String> = listOf("a", "b")

var mutableSetting4: String? = null

fun Dto4.display(): String = name

val Dto4.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull4(): T? = getOrNull(1)

fun createDto4(id: Long, name: String = "dto", vararg tags: String): Dto4 = Dto4(id, name, tags.toList())

inline fun <reified T> typeName4(): String = T::class.java.name

suspend fun loadAll4(service: Service4): List<Dto4> = emptyList()

internal fun internalUtil4(x: Int): Int = x + 1
// FILE: p5/Model5.kt
@file:JvmName("Model5Kt")
package bench.p5

import bench.p4.Dto4
import bench.p4.Status4

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker5(val value: String, val priority: Int = 0)

@Marker5("dto", priority = 5)
data class Dto5(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto4? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto5 = copy(name = newName)
}

enum class Status5(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status5? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result5<out T> {
    data class Ok<T>(val value: T) : Result5<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result5<Nothing>
    object Loading : Result5<Nothing>
}

@JvmInline
value class Id5(val raw: Long)

interface Repo5<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService5<T : Any>(protected val repo: Repo5<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p5/Service5.kt
package bench.p5

import bench.p4.Dto4
import bench.p4.Status4

class Service5(
    repo: Repo5<Dto5, Long>,
    private val clock: () -> Long,
) : BaseService5<Dto5>(repo), Comparable<Service5> {
    override fun validate(entity: Dto5): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto5>, filter: (Dto5) -> Boolean): List<Dto5> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto5> = emptyList()

    suspend fun refresh(force: Boolean): Result5<Dto5> = Result5.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto5) -> R?): List<Dto5> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id5): Dto5? = null

    fun status(dto: Dto5): Status5 = Status5.ACTIVE

    fun copyInto(target: MutableList<in Dto5>, source: List<out Dto5>) {}

    fun subscribe(listener: suspend (Dto5) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto5? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker5("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto5>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service5"

    override fun compareTo(other: Service5): Int = 0

    inner class Session(val token: String) {
        fun service(): Service5 = this@Service5
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 5

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo5<Dto5, Long>): Service5 = Service5(repo) { 0L }

        private val logger = Any()
    }
}

object Registry5 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class TagList5(private val tags: List<String>) : AbstractList<String>() {
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}

// FILE: p5/Utils5.kt
package bench.p5

const val MAX_ITEMS_5: Int = 1000

val defaultTags5: List<String> = listOf("a", "b")

var mutableSetting5: String? = null

fun Dto5.display(): String = name

val Dto5.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull5(): T? = getOrNull(1)

fun createDto5(id: Long, name: String = "dto", vararg tags: String): Dto5 = Dto5(id, name, tags.toList())

inline fun <reified T> typeName5(): String = T::class.java.name

suspend fun loadAll5(service: Service5): List<Dto5> = emptyList()

internal fun internalUtil5(x: Int): Int = x + 1
// FILE: p6/Model6.kt
@file:JvmName("Model6Kt")
package bench.p6

import bench.p5.Dto5
import bench.p5.Status5

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker6(val value: String, val priority: Int = 0)

@Marker6("dto", priority = 6)
data class Dto6(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto5? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto6 = copy(name = newName)
}

enum class Status6(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status6? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result6<out T> {
    data class Ok<T>(val value: T) : Result6<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result6<Nothing>
    object Loading : Result6<Nothing>
}

@JvmInline
value class Id6(val raw: Long)

interface Repo6<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService6<T : Any>(protected val repo: Repo6<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p6/Service6.kt
package bench.p6

import bench.p5.Dto5
import bench.p5.Status5

class Service6(
    repo: Repo6<Dto6, Long>,
    private val clock: () -> Long,
) : BaseService6<Dto6>(repo), Comparable<Service6> {
    override fun validate(entity: Dto6): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto6>, filter: (Dto6) -> Boolean): List<Dto6> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto6> = emptyList()

    suspend fun refresh(force: Boolean): Result6<Dto6> = Result6.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto6) -> R?): List<Dto6> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id6): Dto6? = null

    fun status(dto: Dto6): Status6 = Status6.ACTIVE

    fun copyInto(target: MutableList<in Dto6>, source: List<out Dto6>) {}

    fun subscribe(listener: suspend (Dto6) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto6? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker6("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto6>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service6"

    override fun compareTo(other: Service6): Int = 0

    inner class Session(val token: String) {
        fun service(): Service6 = this@Service6
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 6

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo6<Dto6, Long>): Service6 = Service6(repo) { 0L }

        private val logger = Any()
    }
}

object Registry6 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p6/Utils6.kt
package bench.p6

const val MAX_ITEMS_6: Int = 1000

val defaultTags6: List<String> = listOf("a", "b")

var mutableSetting6: String? = null

fun Dto6.display(): String = name

val Dto6.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull6(): T? = getOrNull(1)

fun createDto6(id: Long, name: String = "dto", vararg tags: String): Dto6 = Dto6(id, name, tags.toList())

inline fun <reified T> typeName6(): String = T::class.java.name

suspend fun loadAll6(service: Service6): List<Dto6> = emptyList()

internal fun internalUtil6(x: Int): Int = x + 1
// FILE: p7/Model7.kt
@file:JvmName("Model7Kt")
package bench.p7

import bench.p6.Dto6
import bench.p6.Status6

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker7(val value: String, val priority: Int = 0)

@Marker7("dto", priority = 7)
data class Dto7(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto6? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto7 = copy(name = newName)
}

enum class Status7(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status7? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result7<out T> {
    data class Ok<T>(val value: T) : Result7<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result7<Nothing>
    object Loading : Result7<Nothing>
}

@JvmInline
value class Id7(val raw: Long)

interface Repo7<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService7<T : Any>(protected val repo: Repo7<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p7/Service7.kt
package bench.p7

import bench.p6.Dto6
import bench.p6.Status6

class Service7(
    repo: Repo7<Dto7, Long>,
    private val clock: () -> Long,
) : BaseService7<Dto7>(repo), Comparable<Service7> {
    override fun validate(entity: Dto7): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto7>, filter: (Dto7) -> Boolean): List<Dto7> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto7> = emptyList()

    suspend fun refresh(force: Boolean): Result7<Dto7> = Result7.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto7) -> R?): List<Dto7> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id7): Dto7? = null

    fun status(dto: Dto7): Status7 = Status7.ACTIVE

    fun copyInto(target: MutableList<in Dto7>, source: List<out Dto7>) {}

    fun subscribe(listener: suspend (Dto7) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto7? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker7("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto7>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service7"

    override fun compareTo(other: Service7): Int = 0

    inner class Session(val token: String) {
        fun service(): Service7 = this@Service7
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 7

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo7<Dto7, Long>): Service7 = Service7(repo) { 0L }

        private val logger = Any()
    }
}

object Registry7 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p7/Utils7.kt
package bench.p7

const val MAX_ITEMS_7: Int = 1000

val defaultTags7: List<String> = listOf("a", "b")

var mutableSetting7: String? = null

fun Dto7.display(): String = name

val Dto7.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull7(): T? = getOrNull(1)

fun createDto7(id: Long, name: String = "dto", vararg tags: String): Dto7 = Dto7(id, name, tags.toList())

inline fun <reified T> typeName7(): String = T::class.java.name

suspend fun loadAll7(service: Service7): List<Dto7> = emptyList()

internal fun internalUtil7(x: Int): Int = x + 1
// FILE: p8/Model8.kt
@file:JvmName("Model8Kt")
package bench.p8

import bench.p7.Dto7
import bench.p7.Status7

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker8(val value: String, val priority: Int = 0)

@Marker8("dto", priority = 8)
data class Dto8(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto7? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto8 = copy(name = newName)
}

enum class Status8(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status8? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result8<out T> {
    data class Ok<T>(val value: T) : Result8<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result8<Nothing>
    object Loading : Result8<Nothing>
}

@JvmInline
value class Id8(val raw: Long)

interface Repo8<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService8<T : Any>(protected val repo: Repo8<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p8/Service8.kt
package bench.p8

import bench.p7.Dto7
import bench.p7.Status7

class Service8(
    repo: Repo8<Dto8, Long>,
    private val clock: () -> Long,
) : BaseService8<Dto8>(repo), Comparable<Service8> {
    override fun validate(entity: Dto8): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto8>, filter: (Dto8) -> Boolean): List<Dto8> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto8> = emptyList()

    suspend fun refresh(force: Boolean): Result8<Dto8> = Result8.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto8) -> R?): List<Dto8> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id8): Dto8? = null

    fun status(dto: Dto8): Status8 = Status8.ACTIVE

    fun copyInto(target: MutableList<in Dto8>, source: List<out Dto8>) {}

    fun subscribe(listener: suspend (Dto8) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto8? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker8("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto8>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service8"

    override fun compareTo(other: Service8): Int = 0

    inner class Session(val token: String) {
        fun service(): Service8 = this@Service8
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 8

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo8<Dto8, Long>): Service8 = Service8(repo) { 0L }

        private val logger = Any()
    }
}

object Registry8 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class LoggingRepo8(private val delegate: Repo8<Dto8, Long>) : Repo8<Dto8, Long> by delegate {
    override fun save(entity: Dto8): Dto8 = delegate.save(entity)
}

// FILE: p8/Utils8.kt
package bench.p8

const val MAX_ITEMS_8: Int = 1000

val defaultTags8: List<String> = listOf("a", "b")

var mutableSetting8: String? = null

fun Dto8.display(): String = name

val Dto8.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull8(): T? = getOrNull(1)

fun createDto8(id: Long, name: String = "dto", vararg tags: String): Dto8 = Dto8(id, name, tags.toList())

inline fun <reified T> typeName8(): String = T::class.java.name

suspend fun loadAll8(service: Service8): List<Dto8> = emptyList()

internal fun internalUtil8(x: Int): Int = x + 1
// FILE: p9/Model9.kt
@file:JvmName("Model9Kt")
package bench.p9

import bench.p8.Dto8
import bench.p8.Status8

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker9(val value: String, val priority: Int = 0)

@Marker9("dto", priority = 9)
data class Dto9(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto8? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto9 = copy(name = newName)
}

enum class Status9(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status9? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result9<out T> {
    data class Ok<T>(val value: T) : Result9<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result9<Nothing>
    object Loading : Result9<Nothing>
}

@JvmInline
value class Id9(val raw: Long)

interface Repo9<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService9<T : Any>(protected val repo: Repo9<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p9/Service9.kt
package bench.p9

import bench.p8.Dto8
import bench.p8.Status8

class Service9(
    repo: Repo9<Dto9, Long>,
    private val clock: () -> Long,
) : BaseService9<Dto9>(repo), Comparable<Service9> {
    override fun validate(entity: Dto9): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto9>, filter: (Dto9) -> Boolean): List<Dto9> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto9> = emptyList()

    suspend fun refresh(force: Boolean): Result9<Dto9> = Result9.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto9) -> R?): List<Dto9> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id9): Dto9? = null

    fun status(dto: Dto9): Status9 = Status9.ACTIVE

    fun copyInto(target: MutableList<in Dto9>, source: List<out Dto9>) {}

    fun subscribe(listener: suspend (Dto9) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto9? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker9("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto9>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service9"

    override fun compareTo(other: Service9): Int = 0

    inner class Session(val token: String) {
        fun service(): Service9 = this@Service9
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 9

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo9<Dto9, Long>): Service9 = Service9(repo) { 0L }

        private val logger = Any()
    }
}

object Registry9 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p9/Utils9.kt
package bench.p9

const val MAX_ITEMS_9: Int = 1000

val defaultTags9: List<String> = listOf("a", "b")

var mutableSetting9: String? = null

fun Dto9.display(): String = name

val Dto9.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull9(): T? = getOrNull(1)

fun createDto9(id: Long, name: String = "dto", vararg tags: String): Dto9 = Dto9(id, name, tags.toList())

inline fun <reified T> typeName9(): String = T::class.java.name

suspend fun loadAll9(service: Service9): List<Dto9> = emptyList()

internal fun internalUtil9(x: Int): Int = x + 1
// FILE: p10/Model10.kt
@file:JvmName("Model10Kt")
package bench.p10

import bench.p9.Dto9
import bench.p9.Status9

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker10(val value: String, val priority: Int = 0)

@Marker10("dto", priority = 10)
data class Dto10(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto9? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto10 = copy(name = newName)
}

enum class Status10(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status10? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result10<out T> {
    data class Ok<T>(val value: T) : Result10<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result10<Nothing>
    object Loading : Result10<Nothing>
}

@JvmInline
value class Id10(val raw: Long)

interface Repo10<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService10<T : Any>(protected val repo: Repo10<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p10/Service10.kt
package bench.p10

import bench.p9.Dto9
import bench.p9.Status9

class Service10(
    repo: Repo10<Dto10, Long>,
    private val clock: () -> Long,
) : BaseService10<Dto10>(repo), Comparable<Service10> {
    override fun validate(entity: Dto10): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto10>, filter: (Dto10) -> Boolean): List<Dto10> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto10> = emptyList()

    suspend fun refresh(force: Boolean): Result10<Dto10> = Result10.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto10) -> R?): List<Dto10> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id10): Dto10? = null

    fun status(dto: Dto10): Status10 = Status10.ACTIVE

    fun copyInto(target: MutableList<in Dto10>, source: List<out Dto10>) {}

    fun subscribe(listener: suspend (Dto10) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto10? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker10("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto10>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service10"

    override fun compareTo(other: Service10): Int = 0

    inner class Session(val token: String) {
        fun service(): Service10 = this@Service10
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 10

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo10<Dto10, Long>): Service10 = Service10(repo) { 0L }

        private val logger = Any()
    }
}

object Registry10 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class TagList10(private val tags: List<String>) : AbstractList<String>() {
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}

// FILE: p10/Utils10.kt
package bench.p10

const val MAX_ITEMS_10: Int = 1000

val defaultTags10: List<String> = listOf("a", "b")

var mutableSetting10: String? = null

fun Dto10.display(): String = name

val Dto10.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull10(): T? = getOrNull(1)

fun createDto10(id: Long, name: String = "dto", vararg tags: String): Dto10 = Dto10(id, name, tags.toList())

inline fun <reified T> typeName10(): String = T::class.java.name

suspend fun loadAll10(service: Service10): List<Dto10> = emptyList()

internal fun internalUtil10(x: Int): Int = x + 1
// FILE: p11/Model11.kt
@file:JvmName("Model11Kt")
package bench.p11

import bench.p10.Dto10
import bench.p10.Status10

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker11(val value: String, val priority: Int = 0)

@Marker11("dto", priority = 11)
data class Dto11(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto10? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto11 = copy(name = newName)
}

enum class Status11(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status11? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result11<out T> {
    data class Ok<T>(val value: T) : Result11<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result11<Nothing>
    object Loading : Result11<Nothing>
}

@JvmInline
value class Id11(val raw: Long)

interface Repo11<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService11<T : Any>(protected val repo: Repo11<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p11/Service11.kt
package bench.p11

import bench.p10.Dto10
import bench.p10.Status10

class Service11(
    repo: Repo11<Dto11, Long>,
    private val clock: () -> Long,
) : BaseService11<Dto11>(repo), Comparable<Service11> {
    override fun validate(entity: Dto11): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto11>, filter: (Dto11) -> Boolean): List<Dto11> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto11> = emptyList()

    suspend fun refresh(force: Boolean): Result11<Dto11> = Result11.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto11) -> R?): List<Dto11> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id11): Dto11? = null

    fun status(dto: Dto11): Status11 = Status11.ACTIVE

    fun copyInto(target: MutableList<in Dto11>, source: List<out Dto11>) {}

    fun subscribe(listener: suspend (Dto11) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto11? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker11("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto11>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service11"

    override fun compareTo(other: Service11): Int = 0

    inner class Session(val token: String) {
        fun service(): Service11 = this@Service11
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 11

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo11<Dto11, Long>): Service11 = Service11(repo) { 0L }

        private val logger = Any()
    }
}

object Registry11 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p11/Utils11.kt
package bench.p11

const val MAX_ITEMS_11: Int = 1000

val defaultTags11: List<String> = listOf("a", "b")

var mutableSetting11: String? = null

fun Dto11.display(): String = name

val Dto11.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull11(): T? = getOrNull(1)

fun createDto11(id: Long, name: String = "dto", vararg tags: String): Dto11 = Dto11(id, name, tags.toList())

inline fun <reified T> typeName11(): String = T::class.java.name

suspend fun loadAll11(service: Service11): List<Dto11> = emptyList()

internal fun internalUtil11(x: Int): Int = x + 1
// FILE: p12/Model12.kt
@file:JvmName("Model12Kt")
package bench.p12

import bench.p11.Dto11
import bench.p11.Status11

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker12(val value: String, val priority: Int = 0)

@Marker12("dto", priority = 12)
data class Dto12(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto11? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto12 = copy(name = newName)
}

enum class Status12(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status12? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result12<out T> {
    data class Ok<T>(val value: T) : Result12<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result12<Nothing>
    object Loading : Result12<Nothing>
}

@JvmInline
value class Id12(val raw: Long)

interface Repo12<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService12<T : Any>(protected val repo: Repo12<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p12/Service12.kt
package bench.p12

import bench.p11.Dto11
import bench.p11.Status11

class Service12(
    repo: Repo12<Dto12, Long>,
    private val clock: () -> Long,
) : BaseService12<Dto12>(repo), Comparable<Service12> {
    override fun validate(entity: Dto12): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto12>, filter: (Dto12) -> Boolean): List<Dto12> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto12> = emptyList()

    suspend fun refresh(force: Boolean): Result12<Dto12> = Result12.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto12) -> R?): List<Dto12> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id12): Dto12? = null

    fun status(dto: Dto12): Status12 = Status12.ACTIVE

    fun copyInto(target: MutableList<in Dto12>, source: List<out Dto12>) {}

    fun subscribe(listener: suspend (Dto12) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto12? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker12("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto12>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service12"

    override fun compareTo(other: Service12): Int = 0

    inner class Session(val token: String) {
        fun service(): Service12 = this@Service12
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 12

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo12<Dto12, Long>): Service12 = Service12(repo) { 0L }

        private val logger = Any()
    }
}

object Registry12 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class LoggingRepo12(private val delegate: Repo12<Dto12, Long>) : Repo12<Dto12, Long> by delegate {
    override fun save(entity: Dto12): Dto12 = delegate.save(entity)
}

// FILE: p12/Utils12.kt
package bench.p12

const val MAX_ITEMS_12: Int = 1000

val defaultTags12: List<String> = listOf("a", "b")

var mutableSetting12: String? = null

fun Dto12.display(): String = name

val Dto12.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull12(): T? = getOrNull(1)

fun createDto12(id: Long, name: String = "dto", vararg tags: String): Dto12 = Dto12(id, name, tags.toList())

inline fun <reified T> typeName12(): String = T::class.java.name

suspend fun loadAll12(service: Service12): List<Dto12> = emptyList()

internal fun internalUtil12(x: Int): Int = x + 1
// FILE: p13/Model13.kt
@file:JvmName("Model13Kt")
package bench.p13

import bench.p12.Dto12
import bench.p12.Status12

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker13(val value: String, val priority: Int = 0)

@Marker13("dto", priority = 13)
data class Dto13(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto12? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto13 = copy(name = newName)
}

enum class Status13(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status13? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result13<out T> {
    data class Ok<T>(val value: T) : Result13<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result13<Nothing>
    object Loading : Result13<Nothing>
}

@JvmInline
value class Id13(val raw: Long)

interface Repo13<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService13<T : Any>(protected val repo: Repo13<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p13/Service13.kt
package bench.p13

import bench.p12.Dto12
import bench.p12.Status12

class Service13(
    repo: Repo13<Dto13, Long>,
    private val clock: () -> Long,
) : BaseService13<Dto13>(repo), Comparable<Service13> {
    override fun validate(entity: Dto13): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto13>, filter: (Dto13) -> Boolean): List<Dto13> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto13> = emptyList()

    suspend fun refresh(force: Boolean): Result13<Dto13> = Result13.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto13) -> R?): List<Dto13> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id13): Dto13? = null

    fun status(dto: Dto13): Status13 = Status13.ACTIVE

    fun copyInto(target: MutableList<in Dto13>, source: List<out Dto13>) {}

    fun subscribe(listener: suspend (Dto13) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto13? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker13("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto13>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service13"

    override fun compareTo(other: Service13): Int = 0

    inner class Session(val token: String) {
        fun service(): Service13 = this@Service13
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 13

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo13<Dto13, Long>): Service13 = Service13(repo) { 0L }

        private val logger = Any()
    }
}

object Registry13 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p13/Utils13.kt
package bench.p13

const val MAX_ITEMS_13: Int = 1000

val defaultTags13: List<String> = listOf("a", "b")

var mutableSetting13: String? = null

fun Dto13.display(): String = name

val Dto13.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull13(): T? = getOrNull(1)

fun createDto13(id: Long, name: String = "dto", vararg tags: String): Dto13 = Dto13(id, name, tags.toList())

inline fun <reified T> typeName13(): String = T::class.java.name

suspend fun loadAll13(service: Service13): List<Dto13> = emptyList()

internal fun internalUtil13(x: Int): Int = x + 1
// FILE: p14/Model14.kt
@file:JvmName("Model14Kt")
package bench.p14

import bench.p13.Dto13
import bench.p13.Status13

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker14(val value: String, val priority: Int = 0)

@Marker14("dto", priority = 14)
data class Dto14(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto13? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto14 = copy(name = newName)
}

enum class Status14(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status14? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result14<out T> {
    data class Ok<T>(val value: T) : Result14<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result14<Nothing>
    object Loading : Result14<Nothing>
}

@JvmInline
value class Id14(val raw: Long)

interface Repo14<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService14<T : Any>(protected val repo: Repo14<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p14/Service14.kt
package bench.p14

import bench.p13.Dto13
import bench.p13.Status13

class Service14(
    repo: Repo14<Dto14, Long>,
    private val clock: () -> Long,
) : BaseService14<Dto14>(repo), Comparable<Service14> {
    override fun validate(entity: Dto14): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto14>, filter: (Dto14) -> Boolean): List<Dto14> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto14> = emptyList()

    suspend fun refresh(force: Boolean): Result14<Dto14> = Result14.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto14) -> R?): List<Dto14> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id14): Dto14? = null

    fun status(dto: Dto14): Status14 = Status14.ACTIVE

    fun copyInto(target: MutableList<in Dto14>, source: List<out Dto14>) {}

    fun subscribe(listener: suspend (Dto14) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto14? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker14("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto14>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service14"

    override fun compareTo(other: Service14): Int = 0

    inner class Session(val token: String) {
        fun service(): Service14 = this@Service14
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 14

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo14<Dto14, Long>): Service14 = Service14(repo) { 0L }

        private val logger = Any()
    }
}

object Registry14 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p14/Utils14.kt
package bench.p14

const val MAX_ITEMS_14: Int = 1000

val defaultTags14: List<String> = listOf("a", "b")

var mutableSetting14: String? = null

fun Dto14.display(): String = name

val Dto14.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull14(): T? = getOrNull(1)

fun createDto14(id: Long, name: String = "dto", vararg tags: String): Dto14 = Dto14(id, name, tags.toList())

inline fun <reified T> typeName14(): String = T::class.java.name

suspend fun loadAll14(service: Service14): List<Dto14> = emptyList()

internal fun internalUtil14(x: Int): Int = x + 1
// FILE: p15/Model15.kt
@file:JvmName("Model15Kt")
package bench.p15

import bench.p14.Dto14
import bench.p14.Status14

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker15(val value: String, val priority: Int = 0)

@Marker15("dto", priority = 15)
data class Dto15(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto14? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto15 = copy(name = newName)
}

enum class Status15(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status15? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result15<out T> {
    data class Ok<T>(val value: T) : Result15<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result15<Nothing>
    object Loading : Result15<Nothing>
}

@JvmInline
value class Id15(val raw: Long)

interface Repo15<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService15<T : Any>(protected val repo: Repo15<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p15/Service15.kt
package bench.p15

import bench.p14.Dto14
import bench.p14.Status14

class Service15(
    repo: Repo15<Dto15, Long>,
    private val clock: () -> Long,
) : BaseService15<Dto15>(repo), Comparable<Service15> {
    override fun validate(entity: Dto15): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto15>, filter: (Dto15) -> Boolean): List<Dto15> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto15> = emptyList()

    suspend fun refresh(force: Boolean): Result15<Dto15> = Result15.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto15) -> R?): List<Dto15> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id15): Dto15? = null

    fun status(dto: Dto15): Status15 = Status15.ACTIVE

    fun copyInto(target: MutableList<in Dto15>, source: List<out Dto15>) {}

    fun subscribe(listener: suspend (Dto15) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto15? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker15("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto15>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service15"

    override fun compareTo(other: Service15): Int = 0

    inner class Session(val token: String) {
        fun service(): Service15 = this@Service15
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 15

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo15<Dto15, Long>): Service15 = Service15(repo) { 0L }

        private val logger = Any()
    }
}

object Registry15 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class TagList15(private val tags: List<String>) : AbstractList<String>() {
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}

// FILE: p15/Utils15.kt
package bench.p15

const val MAX_ITEMS_15: Int = 1000

val defaultTags15: List<String> = listOf("a", "b")

var mutableSetting15: String? = null

fun Dto15.display(): String = name

val Dto15.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull15(): T? = getOrNull(1)

fun createDto15(id: Long, name: String = "dto", vararg tags: String): Dto15 = Dto15(id, name, tags.toList())

inline fun <reified T> typeName15(): String = T::class.java.name

suspend fun loadAll15(service: Service15): List<Dto15> = emptyList()

internal fun internalUtil15(x: Int): Int = x + 1
// FILE: p16/Model16.kt
@file:JvmName("Model16Kt")
package bench.p16

import bench.p15.Dto15
import bench.p15.Status15

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker16(val value: String, val priority: Int = 0)

@Marker16("dto", priority = 16)
data class Dto16(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto15? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto16 = copy(name = newName)
}

enum class Status16(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status16? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result16<out T> {
    data class Ok<T>(val value: T) : Result16<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result16<Nothing>
    object Loading : Result16<Nothing>
}

@JvmInline
value class Id16(val raw: Long)

interface Repo16<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService16<T : Any>(protected val repo: Repo16<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p16/Service16.kt
package bench.p16

import bench.p15.Dto15
import bench.p15.Status15

class Service16(
    repo: Repo16<Dto16, Long>,
    private val clock: () -> Long,
) : BaseService16<Dto16>(repo), Comparable<Service16> {
    override fun validate(entity: Dto16): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto16>, filter: (Dto16) -> Boolean): List<Dto16> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto16> = emptyList()

    suspend fun refresh(force: Boolean): Result16<Dto16> = Result16.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto16) -> R?): List<Dto16> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id16): Dto16? = null

    fun status(dto: Dto16): Status16 = Status16.ACTIVE

    fun copyInto(target: MutableList<in Dto16>, source: List<out Dto16>) {}

    fun subscribe(listener: suspend (Dto16) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto16? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker16("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto16>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service16"

    override fun compareTo(other: Service16): Int = 0

    inner class Session(val token: String) {
        fun service(): Service16 = this@Service16
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 16

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo16<Dto16, Long>): Service16 = Service16(repo) { 0L }

        private val logger = Any()
    }
}

object Registry16 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class LoggingRepo16(private val delegate: Repo16<Dto16, Long>) : Repo16<Dto16, Long> by delegate {
    override fun save(entity: Dto16): Dto16 = delegate.save(entity)
}

// FILE: p16/Utils16.kt
package bench.p16

const val MAX_ITEMS_16: Int = 1000

val defaultTags16: List<String> = listOf("a", "b")

var mutableSetting16: String? = null

fun Dto16.display(): String = name

val Dto16.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull16(): T? = getOrNull(1)

fun createDto16(id: Long, name: String = "dto", vararg tags: String): Dto16 = Dto16(id, name, tags.toList())

inline fun <reified T> typeName16(): String = T::class.java.name

suspend fun loadAll16(service: Service16): List<Dto16> = emptyList()

internal fun internalUtil16(x: Int): Int = x + 1
// FILE: p17/Model17.kt
@file:JvmName("Model17Kt")
package bench.p17

import bench.p16.Dto16
import bench.p16.Status16

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker17(val value: String, val priority: Int = 0)

@Marker17("dto", priority = 17)
data class Dto17(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto16? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto17 = copy(name = newName)
}

enum class Status17(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status17? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result17<out T> {
    data class Ok<T>(val value: T) : Result17<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result17<Nothing>
    object Loading : Result17<Nothing>
}

@JvmInline
value class Id17(val raw: Long)

interface Repo17<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService17<T : Any>(protected val repo: Repo17<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p17/Service17.kt
package bench.p17

import bench.p16.Dto16
import bench.p16.Status16

class Service17(
    repo: Repo17<Dto17, Long>,
    private val clock: () -> Long,
) : BaseService17<Dto17>(repo), Comparable<Service17> {
    override fun validate(entity: Dto17): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto17>, filter: (Dto17) -> Boolean): List<Dto17> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto17> = emptyList()

    suspend fun refresh(force: Boolean): Result17<Dto17> = Result17.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto17) -> R?): List<Dto17> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id17): Dto17? = null

    fun status(dto: Dto17): Status17 = Status17.ACTIVE

    fun copyInto(target: MutableList<in Dto17>, source: List<out Dto17>) {}

    fun subscribe(listener: suspend (Dto17) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto17? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker17("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto17>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service17"

    override fun compareTo(other: Service17): Int = 0

    inner class Session(val token: String) {
        fun service(): Service17 = this@Service17
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 17

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo17<Dto17, Long>): Service17 = Service17(repo) { 0L }

        private val logger = Any()
    }
}

object Registry17 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p17/Utils17.kt
package bench.p17

const val MAX_ITEMS_17: Int = 1000

val defaultTags17: List<String> = listOf("a", "b")

var mutableSetting17: String? = null

fun Dto17.display(): String = name

val Dto17.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull17(): T? = getOrNull(1)

fun createDto17(id: Long, name: String = "dto", vararg tags: String): Dto17 = Dto17(id, name, tags.toList())

inline fun <reified T> typeName17(): String = T::class.java.name

suspend fun loadAll17(service: Service17): List<Dto17> = emptyList()

internal fun internalUtil17(x: Int): Int = x + 1
// FILE: p18/Model18.kt
@file:JvmName("Model18Kt")
package bench.p18

import bench.p17.Dto17
import bench.p17.Status17

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker18(val value: String, val priority: Int = 0)

@Marker18("dto", priority = 18)
data class Dto18(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto17? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto18 = copy(name = newName)
}

enum class Status18(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status18? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result18<out T> {
    data class Ok<T>(val value: T) : Result18<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result18<Nothing>
    object Loading : Result18<Nothing>
}

@JvmInline
value class Id18(val raw: Long)

interface Repo18<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService18<T : Any>(protected val repo: Repo18<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p18/Service18.kt
package bench.p18

import bench.p17.Dto17
import bench.p17.Status17

class Service18(
    repo: Repo18<Dto18, Long>,
    private val clock: () -> Long,
) : BaseService18<Dto18>(repo), Comparable<Service18> {
    override fun validate(entity: Dto18): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto18>, filter: (Dto18) -> Boolean): List<Dto18> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto18> = emptyList()

    suspend fun refresh(force: Boolean): Result18<Dto18> = Result18.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto18) -> R?): List<Dto18> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id18): Dto18? = null

    fun status(dto: Dto18): Status18 = Status18.ACTIVE

    fun copyInto(target: MutableList<in Dto18>, source: List<out Dto18>) {}

    fun subscribe(listener: suspend (Dto18) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto18? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker18("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto18>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service18"

    override fun compareTo(other: Service18): Int = 0

    inner class Session(val token: String) {
        fun service(): Service18 = this@Service18
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 18

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo18<Dto18, Long>): Service18 = Service18(repo) { 0L }

        private val logger = Any()
    }
}

object Registry18 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p18/Utils18.kt
package bench.p18

const val MAX_ITEMS_18: Int = 1000

val defaultTags18: List<String> = listOf("a", "b")

var mutableSetting18: String? = null

fun Dto18.display(): String = name

val Dto18.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull18(): T? = getOrNull(1)

fun createDto18(id: Long, name: String = "dto", vararg tags: String): Dto18 = Dto18(id, name, tags.toList())

inline fun <reified T> typeName18(): String = T::class.java.name

suspend fun loadAll18(service: Service18): List<Dto18> = emptyList()

internal fun internalUtil18(x: Int): Int = x + 1
// FILE: p19/Model19.kt
@file:JvmName("Model19Kt")
package bench.p19

import bench.p18.Dto18
import bench.p18.Status18

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker19(val value: String, val priority: Int = 0)

@Marker19("dto", priority = 19)
data class Dto19(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto18? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto19 = copy(name = newName)
}

enum class Status19(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status19? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result19<out T> {
    data class Ok<T>(val value: T) : Result19<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result19<Nothing>
    object Loading : Result19<Nothing>
}

@JvmInline
value class Id19(val raw: Long)

interface Repo19<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService19<T : Any>(protected val repo: Repo19<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p19/Service19.kt
package bench.p19

import bench.p18.Dto18
import bench.p18.Status18

class Service19(
    repo: Repo19<Dto19, Long>,
    private val clock: () -> Long,
) : BaseService19<Dto19>(repo), Comparable<Service19> {
    override fun validate(entity: Dto19): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto19>, filter: (Dto19) -> Boolean): List<Dto19> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto19> = emptyList()

    suspend fun refresh(force: Boolean): Result19<Dto19> = Result19.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto19) -> R?): List<Dto19> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id19): Dto19? = null

    fun status(dto: Dto19): Status19 = Status19.ACTIVE

    fun copyInto(target: MutableList<in Dto19>, source: List<out Dto19>) {}

    fun subscribe(listener: suspend (Dto19) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto19? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker19("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto19>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service19"

    override fun compareTo(other: Service19): Int = 0

    inner class Session(val token: String) {
        fun service(): Service19 = this@Service19
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 19

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo19<Dto19, Long>): Service19 = Service19(repo) { 0L }

        private val logger = Any()
    }
}

object Registry19 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p19/Utils19.kt
package bench.p19

const val MAX_ITEMS_19: Int = 1000

val defaultTags19: List<String> = listOf("a", "b")

var mutableSetting19: String? = null

fun Dto19.display(): String = name

val Dto19.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull19(): T? = getOrNull(1)

fun createDto19(id: Long, name: String = "dto", vararg tags: String): Dto19 = Dto19(id, name, tags.toList())

inline fun <reified T> typeName19(): String = T::class.java.name

suspend fun loadAll19(service: Service19): List<Dto19> = emptyList()

internal fun internalUtil19(x: Int): Int = x + 1
// FILE: p20/Model20.kt
@file:JvmName("Model20Kt")
package bench.p20

import bench.p19.Dto19
import bench.p19.Status19

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker20(val value: String, val priority: Int = 0)

@Marker20("dto", priority = 20)
data class Dto20(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto19? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto20 = copy(name = newName)
}

enum class Status20(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status20? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result20<out T> {
    data class Ok<T>(val value: T) : Result20<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result20<Nothing>
    object Loading : Result20<Nothing>
}

@JvmInline
value class Id20(val raw: Long)

interface Repo20<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService20<T : Any>(protected val repo: Repo20<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p20/Service20.kt
package bench.p20

import bench.p19.Dto19
import bench.p19.Status19

class Service20(
    repo: Repo20<Dto20, Long>,
    private val clock: () -> Long,
) : BaseService20<Dto20>(repo), Comparable<Service20> {
    override fun validate(entity: Dto20): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto20>, filter: (Dto20) -> Boolean): List<Dto20> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto20> = emptyList()

    suspend fun refresh(force: Boolean): Result20<Dto20> = Result20.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto20) -> R?): List<Dto20> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id20): Dto20? = null

    fun status(dto: Dto20): Status20 = Status20.ACTIVE

    fun copyInto(target: MutableList<in Dto20>, source: List<out Dto20>) {}

    fun subscribe(listener: suspend (Dto20) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto20? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker20("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto20>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service20"

    override fun compareTo(other: Service20): Int = 0

    inner class Session(val token: String) {
        fun service(): Service20 = this@Service20
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 20

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo20<Dto20, Long>): Service20 = Service20(repo) { 0L }

        private val logger = Any()
    }
}

object Registry20 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class TagList20(private val tags: List<String>) : AbstractList<String>() {
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}

class LoggingRepo20(private val delegate: Repo20<Dto20, Long>) : Repo20<Dto20, Long> by delegate {
    override fun save(entity: Dto20): Dto20 = delegate.save(entity)
}

// FILE: p20/Utils20.kt
package bench.p20

const val MAX_ITEMS_20: Int = 1000

val defaultTags20: List<String> = listOf("a", "b")

var mutableSetting20: String? = null

fun Dto20.display(): String = name

val Dto20.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull20(): T? = getOrNull(1)

fun createDto20(id: Long, name: String = "dto", vararg tags: String): Dto20 = Dto20(id, name, tags.toList())

inline fun <reified T> typeName20(): String = T::class.java.name

suspend fun loadAll20(service: Service20): List<Dto20> = emptyList()

internal fun internalUtil20(x: Int): Int = x + 1
// FILE: p21/Model21.kt
@file:JvmName("Model21Kt")
package bench.p21

import bench.p20.Dto20
import bench.p20.Status20

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker21(val value: String, val priority: Int = 0)

@Marker21("dto", priority = 21)
data class Dto21(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto20? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto21 = copy(name = newName)
}

enum class Status21(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status21? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result21<out T> {
    data class Ok<T>(val value: T) : Result21<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result21<Nothing>
    object Loading : Result21<Nothing>
}

@JvmInline
value class Id21(val raw: Long)

interface Repo21<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService21<T : Any>(protected val repo: Repo21<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p21/Service21.kt
package bench.p21

import bench.p20.Dto20
import bench.p20.Status20

class Service21(
    repo: Repo21<Dto21, Long>,
    private val clock: () -> Long,
) : BaseService21<Dto21>(repo), Comparable<Service21> {
    override fun validate(entity: Dto21): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto21>, filter: (Dto21) -> Boolean): List<Dto21> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto21> = emptyList()

    suspend fun refresh(force: Boolean): Result21<Dto21> = Result21.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto21) -> R?): List<Dto21> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id21): Dto21? = null

    fun status(dto: Dto21): Status21 = Status21.ACTIVE

    fun copyInto(target: MutableList<in Dto21>, source: List<out Dto21>) {}

    fun subscribe(listener: suspend (Dto21) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto21? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker21("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto21>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service21"

    override fun compareTo(other: Service21): Int = 0

    inner class Session(val token: String) {
        fun service(): Service21 = this@Service21
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 21

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo21<Dto21, Long>): Service21 = Service21(repo) { 0L }

        private val logger = Any()
    }
}

object Registry21 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p21/Utils21.kt
package bench.p21

const val MAX_ITEMS_21: Int = 1000

val defaultTags21: List<String> = listOf("a", "b")

var mutableSetting21: String? = null

fun Dto21.display(): String = name

val Dto21.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull21(): T? = getOrNull(1)

fun createDto21(id: Long, name: String = "dto", vararg tags: String): Dto21 = Dto21(id, name, tags.toList())

inline fun <reified T> typeName21(): String = T::class.java.name

suspend fun loadAll21(service: Service21): List<Dto21> = emptyList()

internal fun internalUtil21(x: Int): Int = x + 1
// FILE: p22/Model22.kt
@file:JvmName("Model22Kt")
package bench.p22

import bench.p21.Dto21
import bench.p21.Status21

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker22(val value: String, val priority: Int = 0)

@Marker22("dto", priority = 22)
data class Dto22(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto21? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto22 = copy(name = newName)
}

enum class Status22(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status22? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result22<out T> {
    data class Ok<T>(val value: T) : Result22<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result22<Nothing>
    object Loading : Result22<Nothing>
}

@JvmInline
value class Id22(val raw: Long)

interface Repo22<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService22<T : Any>(protected val repo: Repo22<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p22/Service22.kt
package bench.p22

import bench.p21.Dto21
import bench.p21.Status21

class Service22(
    repo: Repo22<Dto22, Long>,
    private val clock: () -> Long,
) : BaseService22<Dto22>(repo), Comparable<Service22> {
    override fun validate(entity: Dto22): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto22>, filter: (Dto22) -> Boolean): List<Dto22> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto22> = emptyList()

    suspend fun refresh(force: Boolean): Result22<Dto22> = Result22.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto22) -> R?): List<Dto22> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id22): Dto22? = null

    fun status(dto: Dto22): Status22 = Status22.ACTIVE

    fun copyInto(target: MutableList<in Dto22>, source: List<out Dto22>) {}

    fun subscribe(listener: suspend (Dto22) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto22? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker22("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto22>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service22"

    override fun compareTo(other: Service22): Int = 0

    inner class Session(val token: String) {
        fun service(): Service22 = this@Service22
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 22

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo22<Dto22, Long>): Service22 = Service22(repo) { 0L }

        private val logger = Any()
    }
}

object Registry22 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p22/Utils22.kt
package bench.p22

const val MAX_ITEMS_22: Int = 1000

val defaultTags22: List<String> = listOf("a", "b")

var mutableSetting22: String? = null

fun Dto22.display(): String = name

val Dto22.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull22(): T? = getOrNull(1)

fun createDto22(id: Long, name: String = "dto", vararg tags: String): Dto22 = Dto22(id, name, tags.toList())

inline fun <reified T> typeName22(): String = T::class.java.name

suspend fun loadAll22(service: Service22): List<Dto22> = emptyList()

internal fun internalUtil22(x: Int): Int = x + 1
// FILE: p23/Model23.kt
@file:JvmName("Model23Kt")
package bench.p23

import bench.p22.Dto22
import bench.p22.Status22

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker23(val value: String, val priority: Int = 0)

@Marker23("dto", priority = 23)
data class Dto23(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto22? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto23 = copy(name = newName)
}

enum class Status23(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status23? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result23<out T> {
    data class Ok<T>(val value: T) : Result23<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result23<Nothing>
    object Loading : Result23<Nothing>
}

@JvmInline
value class Id23(val raw: Long)

interface Repo23<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService23<T : Any>(protected val repo: Repo23<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p23/Service23.kt
package bench.p23

import bench.p22.Dto22
import bench.p22.Status22

class Service23(
    repo: Repo23<Dto23, Long>,
    private val clock: () -> Long,
) : BaseService23<Dto23>(repo), Comparable<Service23> {
    override fun validate(entity: Dto23): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto23>, filter: (Dto23) -> Boolean): List<Dto23> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto23> = emptyList()

    suspend fun refresh(force: Boolean): Result23<Dto23> = Result23.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto23) -> R?): List<Dto23> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id23): Dto23? = null

    fun status(dto: Dto23): Status23 = Status23.ACTIVE

    fun copyInto(target: MutableList<in Dto23>, source: List<out Dto23>) {}

    fun subscribe(listener: suspend (Dto23) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto23? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker23("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto23>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service23"

    override fun compareTo(other: Service23): Int = 0

    inner class Session(val token: String) {
        fun service(): Service23 = this@Service23
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 23

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo23<Dto23, Long>): Service23 = Service23(repo) { 0L }

        private val logger = Any()
    }
}

object Registry23 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p23/Utils23.kt
package bench.p23

const val MAX_ITEMS_23: Int = 1000

val defaultTags23: List<String> = listOf("a", "b")

var mutableSetting23: String? = null

fun Dto23.display(): String = name

val Dto23.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull23(): T? = getOrNull(1)

fun createDto23(id: Long, name: String = "dto", vararg tags: String): Dto23 = Dto23(id, name, tags.toList())

inline fun <reified T> typeName23(): String = T::class.java.name

suspend fun loadAll23(service: Service23): List<Dto23> = emptyList()

internal fun internalUtil23(x: Int): Int = x + 1
// FILE: p24/Model24.kt
@file:JvmName("Model24Kt")
package bench.p24

import bench.p23.Dto23
import bench.p23.Status23

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker24(val value: String, val priority: Int = 0)

@Marker24("dto", priority = 24)
data class Dto24(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto23? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto24 = copy(name = newName)
}

enum class Status24(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status24? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result24<out T> {
    data class Ok<T>(val value: T) : Result24<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result24<Nothing>
    object Loading : Result24<Nothing>
}

@JvmInline
value class Id24(val raw: Long)

interface Repo24<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService24<T : Any>(protected val repo: Repo24<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p24/Service24.kt
package bench.p24

import bench.p23.Dto23
import bench.p23.Status23

class Service24(
    repo: Repo24<Dto24, Long>,
    private val clock: () -> Long,
) : BaseService24<Dto24>(repo), Comparable<Service24> {
    override fun validate(entity: Dto24): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto24>, filter: (Dto24) -> Boolean): List<Dto24> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto24> = emptyList()

    suspend fun refresh(force: Boolean): Result24<Dto24> = Result24.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto24) -> R?): List<Dto24> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id24): Dto24? = null

    fun status(dto: Dto24): Status24 = Status24.ACTIVE

    fun copyInto(target: MutableList<in Dto24>, source: List<out Dto24>) {}

    fun subscribe(listener: suspend (Dto24) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto24? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker24("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto24>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service24"

    override fun compareTo(other: Service24): Int = 0

    inner class Session(val token: String) {
        fun service(): Service24 = this@Service24
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 24

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo24<Dto24, Long>): Service24 = Service24(repo) { 0L }

        private val logger = Any()
    }
}

object Registry24 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class LoggingRepo24(private val delegate: Repo24<Dto24, Long>) : Repo24<Dto24, Long> by delegate {
    override fun save(entity: Dto24): Dto24 = delegate.save(entity)
}

// FILE: p24/Utils24.kt
package bench.p24

const val MAX_ITEMS_24: Int = 1000

val defaultTags24: List<String> = listOf("a", "b")

var mutableSetting24: String? = null

fun Dto24.display(): String = name

val Dto24.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull24(): T? = getOrNull(1)

fun createDto24(id: Long, name: String = "dto", vararg tags: String): Dto24 = Dto24(id, name, tags.toList())

inline fun <reified T> typeName24(): String = T::class.java.name

suspend fun loadAll24(service: Service24): List<Dto24> = emptyList()

internal fun internalUtil24(x: Int): Int = x + 1
// FILE: p25/Model25.kt
@file:JvmName("Model25Kt")
package bench.p25

import bench.p24.Dto24
import bench.p24.Status24

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker25(val value: String, val priority: Int = 0)

@Marker25("dto", priority = 25)
data class Dto25(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto24? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto25 = copy(name = newName)
}

enum class Status25(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status25? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result25<out T> {
    data class Ok<T>(val value: T) : Result25<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result25<Nothing>
    object Loading : Result25<Nothing>
}

@JvmInline
value class Id25(val raw: Long)

interface Repo25<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService25<T : Any>(protected val repo: Repo25<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p25/Service25.kt
package bench.p25

import bench.p24.Dto24
import bench.p24.Status24

class Service25(
    repo: Repo25<Dto25, Long>,
    private val clock: () -> Long,
) : BaseService25<Dto25>(repo), Comparable<Service25> {
    override fun validate(entity: Dto25): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto25>, filter: (Dto25) -> Boolean): List<Dto25> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto25> = emptyList()

    suspend fun refresh(force: Boolean): Result25<Dto25> = Result25.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto25) -> R?): List<Dto25> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id25): Dto25? = null

    fun status(dto: Dto25): Status25 = Status25.ACTIVE

    fun copyInto(target: MutableList<in Dto25>, source: List<out Dto25>) {}

    fun subscribe(listener: suspend (Dto25) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto25? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker25("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto25>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service25"

    override fun compareTo(other: Service25): Int = 0

    inner class Session(val token: String) {
        fun service(): Service25 = this@Service25
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 25

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo25<Dto25, Long>): Service25 = Service25(repo) { 0L }

        private val logger = Any()
    }
}

object Registry25 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class TagList25(private val tags: List<String>) : AbstractList<String>() {
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}

// FILE: p25/Utils25.kt
package bench.p25

const val MAX_ITEMS_25: Int = 1000

val defaultTags25: List<String> = listOf("a", "b")

var mutableSetting25: String? = null

fun Dto25.display(): String = name

val Dto25.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull25(): T? = getOrNull(1)

fun createDto25(id: Long, name: String = "dto", vararg tags: String): Dto25 = Dto25(id, name, tags.toList())

inline fun <reified T> typeName25(): String = T::class.java.name

suspend fun loadAll25(service: Service25): List<Dto25> = emptyList()

internal fun internalUtil25(x: Int): Int = x + 1
// FILE: p26/Model26.kt
@file:JvmName("Model26Kt")
package bench.p26

import bench.p25.Dto25
import bench.p25.Status25

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker26(val value: String, val priority: Int = 0)

@Marker26("dto", priority = 26)
data class Dto26(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto25? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto26 = copy(name = newName)
}

enum class Status26(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status26? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result26<out T> {
    data class Ok<T>(val value: T) : Result26<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result26<Nothing>
    object Loading : Result26<Nothing>
}

@JvmInline
value class Id26(val raw: Long)

interface Repo26<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService26<T : Any>(protected val repo: Repo26<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p26/Service26.kt
package bench.p26

import bench.p25.Dto25
import bench.p25.Status25

class Service26(
    repo: Repo26<Dto26, Long>,
    private val clock: () -> Long,
) : BaseService26<Dto26>(repo), Comparable<Service26> {
    override fun validate(entity: Dto26): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto26>, filter: (Dto26) -> Boolean): List<Dto26> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto26> = emptyList()

    suspend fun refresh(force: Boolean): Result26<Dto26> = Result26.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto26) -> R?): List<Dto26> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id26): Dto26? = null

    fun status(dto: Dto26): Status26 = Status26.ACTIVE

    fun copyInto(target: MutableList<in Dto26>, source: List<out Dto26>) {}

    fun subscribe(listener: suspend (Dto26) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto26? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker26("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto26>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service26"

    override fun compareTo(other: Service26): Int = 0

    inner class Session(val token: String) {
        fun service(): Service26 = this@Service26
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 26

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo26<Dto26, Long>): Service26 = Service26(repo) { 0L }

        private val logger = Any()
    }
}

object Registry26 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p26/Utils26.kt
package bench.p26

const val MAX_ITEMS_26: Int = 1000

val defaultTags26: List<String> = listOf("a", "b")

var mutableSetting26: String? = null

fun Dto26.display(): String = name

val Dto26.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull26(): T? = getOrNull(1)

fun createDto26(id: Long, name: String = "dto", vararg tags: String): Dto26 = Dto26(id, name, tags.toList())

inline fun <reified T> typeName26(): String = T::class.java.name

suspend fun loadAll26(service: Service26): List<Dto26> = emptyList()

internal fun internalUtil26(x: Int): Int = x + 1
// FILE: p27/Model27.kt
@file:JvmName("Model27Kt")
package bench.p27

import bench.p26.Dto26
import bench.p26.Status26

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker27(val value: String, val priority: Int = 0)

@Marker27("dto", priority = 27)
data class Dto27(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto26? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto27 = copy(name = newName)
}

enum class Status27(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status27? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result27<out T> {
    data class Ok<T>(val value: T) : Result27<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result27<Nothing>
    object Loading : Result27<Nothing>
}

@JvmInline
value class Id27(val raw: Long)

interface Repo27<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService27<T : Any>(protected val repo: Repo27<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p27/Service27.kt
package bench.p27

import bench.p26.Dto26
import bench.p26.Status26

class Service27(
    repo: Repo27<Dto27, Long>,
    private val clock: () -> Long,
) : BaseService27<Dto27>(repo), Comparable<Service27> {
    override fun validate(entity: Dto27): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto27>, filter: (Dto27) -> Boolean): List<Dto27> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto27> = emptyList()

    suspend fun refresh(force: Boolean): Result27<Dto27> = Result27.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto27) -> R?): List<Dto27> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id27): Dto27? = null

    fun status(dto: Dto27): Status27 = Status27.ACTIVE

    fun copyInto(target: MutableList<in Dto27>, source: List<out Dto27>) {}

    fun subscribe(listener: suspend (Dto27) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto27? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker27("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto27>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service27"

    override fun compareTo(other: Service27): Int = 0

    inner class Session(val token: String) {
        fun service(): Service27 = this@Service27
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 27

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo27<Dto27, Long>): Service27 = Service27(repo) { 0L }

        private val logger = Any()
    }
}

object Registry27 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p27/Utils27.kt
package bench.p27

const val MAX_ITEMS_27: Int = 1000

val defaultTags27: List<String> = listOf("a", "b")

var mutableSetting27: String? = null

fun Dto27.display(): String = name

val Dto27.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull27(): T? = getOrNull(1)

fun createDto27(id: Long, name: String = "dto", vararg tags: String): Dto27 = Dto27(id, name, tags.toList())

inline fun <reified T> typeName27(): String = T::class.java.name

suspend fun loadAll27(service: Service27): List<Dto27> = emptyList()

internal fun internalUtil27(x: Int): Int = x + 1
// FILE: p28/Model28.kt
@file:JvmName("Model28Kt")
package bench.p28

import bench.p27.Dto27
import bench.p27.Status27

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker28(val value: String, val priority: Int = 0)

@Marker28("dto", priority = 28)
data class Dto28(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto27? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto28 = copy(name = newName)
}

enum class Status28(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status28? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result28<out T> {
    data class Ok<T>(val value: T) : Result28<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result28<Nothing>
    object Loading : Result28<Nothing>
}

@JvmInline
value class Id28(val raw: Long)

interface Repo28<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService28<T : Any>(protected val repo: Repo28<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p28/Service28.kt
package bench.p28

import bench.p27.Dto27
import bench.p27.Status27

class Service28(
    repo: Repo28<Dto28, Long>,
    private val clock: () -> Long,
) : BaseService28<Dto28>(repo), Comparable<Service28> {
    override fun validate(entity: Dto28): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto28>, filter: (Dto28) -> Boolean): List<Dto28> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto28> = emptyList()

    suspend fun refresh(force: Boolean): Result28<Dto28> = Result28.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto28) -> R?): List<Dto28> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id28): Dto28? = null

    fun status(dto: Dto28): Status28 = Status28.ACTIVE

    fun copyInto(target: MutableList<in Dto28>, source: List<out Dto28>) {}

    fun subscribe(listener: suspend (Dto28) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto28? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker28("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto28>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service28"

    override fun compareTo(other: Service28): Int = 0

    inner class Session(val token: String) {
        fun service(): Service28 = this@Service28
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 28

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo28<Dto28, Long>): Service28 = Service28(repo) { 0L }

        private val logger = Any()
    }
}

object Registry28 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class LoggingRepo28(private val delegate: Repo28<Dto28, Long>) : Repo28<Dto28, Long> by delegate {
    override fun save(entity: Dto28): Dto28 = delegate.save(entity)
}

// FILE: p28/Utils28.kt
package bench.p28

const val MAX_ITEMS_28: Int = 1000

val defaultTags28: List<String> = listOf("a", "b")

var mutableSetting28: String? = null

fun Dto28.display(): String = name

val Dto28.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull28(): T? = getOrNull(1)

fun createDto28(id: Long, name: String = "dto", vararg tags: String): Dto28 = Dto28(id, name, tags.toList())

inline fun <reified T> typeName28(): String = T::class.java.name

suspend fun loadAll28(service: Service28): List<Dto28> = emptyList()

internal fun internalUtil28(x: Int): Int = x + 1
// FILE: p29/Model29.kt
@file:JvmName("Model29Kt")
package bench.p29

import bench.p28.Dto28
import bench.p28.Status28

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker29(val value: String, val priority: Int = 0)

@Marker29("dto", priority = 29)
data class Dto29(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto28? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto29 = copy(name = newName)
}

enum class Status29(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status29? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result29<out T> {
    data class Ok<T>(val value: T) : Result29<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result29<Nothing>
    object Loading : Result29<Nothing>
}

@JvmInline
value class Id29(val raw: Long)

interface Repo29<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService29<T : Any>(protected val repo: Repo29<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p29/Service29.kt
package bench.p29

import bench.p28.Dto28
import bench.p28.Status28

class Service29(
    repo: Repo29<Dto29, Long>,
    private val clock: () -> Long,
) : BaseService29<Dto29>(repo), Comparable<Service29> {
    override fun validate(entity: Dto29): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto29>, filter: (Dto29) -> Boolean): List<Dto29> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto29> = emptyList()

    suspend fun refresh(force: Boolean): Result29<Dto29> = Result29.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto29) -> R?): List<Dto29> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id29): Dto29? = null

    fun status(dto: Dto29): Status29 = Status29.ACTIVE

    fun copyInto(target: MutableList<in Dto29>, source: List<out Dto29>) {}

    fun subscribe(listener: suspend (Dto29) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto29? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker29("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto29>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service29"

    override fun compareTo(other: Service29): Int = 0

    inner class Session(val token: String) {
        fun service(): Service29 = this@Service29
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 29

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo29<Dto29, Long>): Service29 = Service29(repo) { 0L }

        private val logger = Any()
    }
}

object Registry29 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p29/Utils29.kt
package bench.p29

const val MAX_ITEMS_29: Int = 1000

val defaultTags29: List<String> = listOf("a", "b")

var mutableSetting29: String? = null

fun Dto29.display(): String = name

val Dto29.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull29(): T? = getOrNull(1)

fun createDto29(id: Long, name: String = "dto", vararg tags: String): Dto29 = Dto29(id, name, tags.toList())

inline fun <reified T> typeName29(): String = T::class.java.name

suspend fun loadAll29(service: Service29): List<Dto29> = emptyList()

internal fun internalUtil29(x: Int): Int = x + 1
// FILE: p30/Model30.kt
@file:JvmName("Model30Kt")
package bench.p30

import bench.p29.Dto29
import bench.p29.Status29

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker30(val value: String, val priority: Int = 0)

@Marker30("dto", priority = 30)
data class Dto30(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto29? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto30 = copy(name = newName)
}

enum class Status30(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status30? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result30<out T> {
    data class Ok<T>(val value: T) : Result30<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result30<Nothing>
    object Loading : Result30<Nothing>
}

@JvmInline
value class Id30(val raw: Long)

interface Repo30<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService30<T : Any>(protected val repo: Repo30<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p30/Service30.kt
package bench.p30

import bench.p29.Dto29
import bench.p29.Status29

class Service30(
    repo: Repo30<Dto30, Long>,
    private val clock: () -> Long,
) : BaseService30<Dto30>(repo), Comparable<Service30> {
    override fun validate(entity: Dto30): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto30>, filter: (Dto30) -> Boolean): List<Dto30> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto30> = emptyList()

    suspend fun refresh(force: Boolean): Result30<Dto30> = Result30.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto30) -> R?): List<Dto30> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id30): Dto30? = null

    fun status(dto: Dto30): Status30 = Status30.ACTIVE

    fun copyInto(target: MutableList<in Dto30>, source: List<out Dto30>) {}

    fun subscribe(listener: suspend (Dto30) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto30? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker30("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto30>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service30"

    override fun compareTo(other: Service30): Int = 0

    inner class Session(val token: String) {
        fun service(): Service30 = this@Service30
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 30

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo30<Dto30, Long>): Service30 = Service30(repo) { 0L }

        private val logger = Any()
    }
}

object Registry30 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class TagList30(private val tags: List<String>) : AbstractList<String>() {
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}

// FILE: p30/Utils30.kt
package bench.p30

const val MAX_ITEMS_30: Int = 1000

val defaultTags30: List<String> = listOf("a", "b")

var mutableSetting30: String? = null

fun Dto30.display(): String = name

val Dto30.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull30(): T? = getOrNull(1)

fun createDto30(id: Long, name: String = "dto", vararg tags: String): Dto30 = Dto30(id, name, tags.toList())

inline fun <reified T> typeName30(): String = T::class.java.name

suspend fun loadAll30(service: Service30): List<Dto30> = emptyList()

internal fun internalUtil30(x: Int): Int = x + 1
// FILE: p31/Model31.kt
@file:JvmName("Model31Kt")
package bench.p31

import bench.p30.Dto30
import bench.p30.Status30

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker31(val value: String, val priority: Int = 0)

@Marker31("dto", priority = 31)
data class Dto31(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto30? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto31 = copy(name = newName)
}

enum class Status31(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status31? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result31<out T> {
    data class Ok<T>(val value: T) : Result31<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result31<Nothing>
    object Loading : Result31<Nothing>
}

@JvmInline
value class Id31(val raw: Long)

interface Repo31<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService31<T : Any>(protected val repo: Repo31<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p31/Service31.kt
package bench.p31

import bench.p30.Dto30
import bench.p30.Status30

class Service31(
    repo: Repo31<Dto31, Long>,
    private val clock: () -> Long,
) : BaseService31<Dto31>(repo), Comparable<Service31> {
    override fun validate(entity: Dto31): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto31>, filter: (Dto31) -> Boolean): List<Dto31> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto31> = emptyList()

    suspend fun refresh(force: Boolean): Result31<Dto31> = Result31.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto31) -> R?): List<Dto31> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id31): Dto31? = null

    fun status(dto: Dto31): Status31 = Status31.ACTIVE

    fun copyInto(target: MutableList<in Dto31>, source: List<out Dto31>) {}

    fun subscribe(listener: suspend (Dto31) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto31? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker31("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto31>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service31"

    override fun compareTo(other: Service31): Int = 0

    inner class Session(val token: String) {
        fun service(): Service31 = this@Service31
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 31

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo31<Dto31, Long>): Service31 = Service31(repo) { 0L }

        private val logger = Any()
    }
}

object Registry31 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p31/Utils31.kt
package bench.p31

const val MAX_ITEMS_31: Int = 1000

val defaultTags31: List<String> = listOf("a", "b")

var mutableSetting31: String? = null

fun Dto31.display(): String = name

val Dto31.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull31(): T? = getOrNull(1)

fun createDto31(id: Long, name: String = "dto", vararg tags: String): Dto31 = Dto31(id, name, tags.toList())

inline fun <reified T> typeName31(): String = T::class.java.name

suspend fun loadAll31(service: Service31): List<Dto31> = emptyList()

internal fun internalUtil31(x: Int): Int = x + 1
// FILE: p32/Model32.kt
@file:JvmName("Model32Kt")
package bench.p32

import bench.p31.Dto31
import bench.p31.Status31

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker32(val value: String, val priority: Int = 0)

@Marker32("dto", priority = 32)
data class Dto32(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto31? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto32 = copy(name = newName)
}

enum class Status32(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status32? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result32<out T> {
    data class Ok<T>(val value: T) : Result32<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result32<Nothing>
    object Loading : Result32<Nothing>
}

@JvmInline
value class Id32(val raw: Long)

interface Repo32<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService32<T : Any>(protected val repo: Repo32<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p32/Service32.kt
package bench.p32

import bench.p31.Dto31
import bench.p31.Status31

class Service32(
    repo: Repo32<Dto32, Long>,
    private val clock: () -> Long,
) : BaseService32<Dto32>(repo), Comparable<Service32> {
    override fun validate(entity: Dto32): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto32>, filter: (Dto32) -> Boolean): List<Dto32> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto32> = emptyList()

    suspend fun refresh(force: Boolean): Result32<Dto32> = Result32.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto32) -> R?): List<Dto32> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id32): Dto32? = null

    fun status(dto: Dto32): Status32 = Status32.ACTIVE

    fun copyInto(target: MutableList<in Dto32>, source: List<out Dto32>) {}

    fun subscribe(listener: suspend (Dto32) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto32? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker32("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto32>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service32"

    override fun compareTo(other: Service32): Int = 0

    inner class Session(val token: String) {
        fun service(): Service32 = this@Service32
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 32

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo32<Dto32, Long>): Service32 = Service32(repo) { 0L }

        private val logger = Any()
    }
}

object Registry32 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class LoggingRepo32(private val delegate: Repo32<Dto32, Long>) : Repo32<Dto32, Long> by delegate {
    override fun save(entity: Dto32): Dto32 = delegate.save(entity)
}

// FILE: p32/Utils32.kt
package bench.p32

const val MAX_ITEMS_32: Int = 1000

val defaultTags32: List<String> = listOf("a", "b")

var mutableSetting32: String? = null

fun Dto32.display(): String = name

val Dto32.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull32(): T? = getOrNull(1)

fun createDto32(id: Long, name: String = "dto", vararg tags: String): Dto32 = Dto32(id, name, tags.toList())

inline fun <reified T> typeName32(): String = T::class.java.name

suspend fun loadAll32(service: Service32): List<Dto32> = emptyList()

internal fun internalUtil32(x: Int): Int = x + 1
// FILE: p33/Model33.kt
@file:JvmName("Model33Kt")
package bench.p33

import bench.p32.Dto32
import bench.p32.Status32

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker33(val value: String, val priority: Int = 0)

@Marker33("dto", priority = 33)
data class Dto33(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto32? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto33 = copy(name = newName)
}

enum class Status33(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status33? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result33<out T> {
    data class Ok<T>(val value: T) : Result33<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result33<Nothing>
    object Loading : Result33<Nothing>
}

@JvmInline
value class Id33(val raw: Long)

interface Repo33<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService33<T : Any>(protected val repo: Repo33<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p33/Service33.kt
package bench.p33

import bench.p32.Dto32
import bench.p32.Status32

class Service33(
    repo: Repo33<Dto33, Long>,
    private val clock: () -> Long,
) : BaseService33<Dto33>(repo), Comparable<Service33> {
    override fun validate(entity: Dto33): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto33>, filter: (Dto33) -> Boolean): List<Dto33> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto33> = emptyList()

    suspend fun refresh(force: Boolean): Result33<Dto33> = Result33.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto33) -> R?): List<Dto33> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id33): Dto33? = null

    fun status(dto: Dto33): Status33 = Status33.ACTIVE

    fun copyInto(target: MutableList<in Dto33>, source: List<out Dto33>) {}

    fun subscribe(listener: suspend (Dto33) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto33? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker33("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto33>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service33"

    override fun compareTo(other: Service33): Int = 0

    inner class Session(val token: String) {
        fun service(): Service33 = this@Service33
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 33

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo33<Dto33, Long>): Service33 = Service33(repo) { 0L }

        private val logger = Any()
    }
}

object Registry33 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p33/Utils33.kt
package bench.p33

const val MAX_ITEMS_33: Int = 1000

val defaultTags33: List<String> = listOf("a", "b")

var mutableSetting33: String? = null

fun Dto33.display(): String = name

val Dto33.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull33(): T? = getOrNull(1)

fun createDto33(id: Long, name: String = "dto", vararg tags: String): Dto33 = Dto33(id, name, tags.toList())

inline fun <reified T> typeName33(): String = T::class.java.name

suspend fun loadAll33(service: Service33): List<Dto33> = emptyList()

internal fun internalUtil33(x: Int): Int = x + 1
// FILE: p34/Model34.kt
@file:JvmName("Model34Kt")
package bench.p34

import bench.p33.Dto33
import bench.p33.Status33

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker34(val value: String, val priority: Int = 0)

@Marker34("dto", priority = 34)
data class Dto34(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto33? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto34 = copy(name = newName)
}

enum class Status34(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status34? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result34<out T> {
    data class Ok<T>(val value: T) : Result34<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result34<Nothing>
    object Loading : Result34<Nothing>
}

@JvmInline
value class Id34(val raw: Long)

interface Repo34<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService34<T : Any>(protected val repo: Repo34<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p34/Service34.kt
package bench.p34

import bench.p33.Dto33
import bench.p33.Status33

class Service34(
    repo: Repo34<Dto34, Long>,
    private val clock: () -> Long,
) : BaseService34<Dto34>(repo), Comparable<Service34> {
    override fun validate(entity: Dto34): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto34>, filter: (Dto34) -> Boolean): List<Dto34> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto34> = emptyList()

    suspend fun refresh(force: Boolean): Result34<Dto34> = Result34.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto34) -> R?): List<Dto34> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id34): Dto34? = null

    fun status(dto: Dto34): Status34 = Status34.ACTIVE

    fun copyInto(target: MutableList<in Dto34>, source: List<out Dto34>) {}

    fun subscribe(listener: suspend (Dto34) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto34? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker34("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto34>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service34"

    override fun compareTo(other: Service34): Int = 0

    inner class Session(val token: String) {
        fun service(): Service34 = this@Service34
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 34

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo34<Dto34, Long>): Service34 = Service34(repo) { 0L }

        private val logger = Any()
    }
}

object Registry34 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p34/Utils34.kt
package bench.p34

const val MAX_ITEMS_34: Int = 1000

val defaultTags34: List<String> = listOf("a", "b")

var mutableSetting34: String? = null

fun Dto34.display(): String = name

val Dto34.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull34(): T? = getOrNull(1)

fun createDto34(id: Long, name: String = "dto", vararg tags: String): Dto34 = Dto34(id, name, tags.toList())

inline fun <reified T> typeName34(): String = T::class.java.name

suspend fun loadAll34(service: Service34): List<Dto34> = emptyList()

internal fun internalUtil34(x: Int): Int = x + 1
// FILE: p35/Model35.kt
@file:JvmName("Model35Kt")
package bench.p35

import bench.p34.Dto34
import bench.p34.Status34

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker35(val value: String, val priority: Int = 0)

@Marker35("dto", priority = 35)
data class Dto35(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto34? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto35 = copy(name = newName)
}

enum class Status35(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status35? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result35<out T> {
    data class Ok<T>(val value: T) : Result35<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result35<Nothing>
    object Loading : Result35<Nothing>
}

@JvmInline
value class Id35(val raw: Long)

interface Repo35<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService35<T : Any>(protected val repo: Repo35<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p35/Service35.kt
package bench.p35

import bench.p34.Dto34
import bench.p34.Status34

class Service35(
    repo: Repo35<Dto35, Long>,
    private val clock: () -> Long,
) : BaseService35<Dto35>(repo), Comparable<Service35> {
    override fun validate(entity: Dto35): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto35>, filter: (Dto35) -> Boolean): List<Dto35> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto35> = emptyList()

    suspend fun refresh(force: Boolean): Result35<Dto35> = Result35.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto35) -> R?): List<Dto35> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id35): Dto35? = null

    fun status(dto: Dto35): Status35 = Status35.ACTIVE

    fun copyInto(target: MutableList<in Dto35>, source: List<out Dto35>) {}

    fun subscribe(listener: suspend (Dto35) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto35? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker35("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto35>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service35"

    override fun compareTo(other: Service35): Int = 0

    inner class Session(val token: String) {
        fun service(): Service35 = this@Service35
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 35

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo35<Dto35, Long>): Service35 = Service35(repo) { 0L }

        private val logger = Any()
    }
}

object Registry35 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class TagList35(private val tags: List<String>) : AbstractList<String>() {
    override val size: Int get() = tags.size
    override fun get(index: Int): String = tags[index]
}

// FILE: p35/Utils35.kt
package bench.p35

const val MAX_ITEMS_35: Int = 1000

val defaultTags35: List<String> = listOf("a", "b")

var mutableSetting35: String? = null

fun Dto35.display(): String = name

val Dto35.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull35(): T? = getOrNull(1)

fun createDto35(id: Long, name: String = "dto", vararg tags: String): Dto35 = Dto35(id, name, tags.toList())

inline fun <reified T> typeName35(): String = T::class.java.name

suspend fun loadAll35(service: Service35): List<Dto35> = emptyList()

internal fun internalUtil35(x: Int): Int = x + 1
// FILE: p36/Model36.kt
@file:JvmName("Model36Kt")
package bench.p36

import bench.p35.Dto35
import bench.p35.Status35

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker36(val value: String, val priority: Int = 0)

@Marker36("dto", priority = 36)
data class Dto36(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto35? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto36 = copy(name = newName)
}

enum class Status36(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status36? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result36<out T> {
    data class Ok<T>(val value: T) : Result36<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result36<Nothing>
    object Loading : Result36<Nothing>
}

@JvmInline
value class Id36(val raw: Long)

interface Repo36<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService36<T : Any>(protected val repo: Repo36<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p36/Service36.kt
package bench.p36

import bench.p35.Dto35
import bench.p35.Status35

class Service36(
    repo: Repo36<Dto36, Long>,
    private val clock: () -> Long,
) : BaseService36<Dto36>(repo), Comparable<Service36> {
    override fun validate(entity: Dto36): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto36>, filter: (Dto36) -> Boolean): List<Dto36> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto36> = emptyList()

    suspend fun refresh(force: Boolean): Result36<Dto36> = Result36.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto36) -> R?): List<Dto36> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id36): Dto36? = null

    fun status(dto: Dto36): Status36 = Status36.ACTIVE

    fun copyInto(target: MutableList<in Dto36>, source: List<out Dto36>) {}

    fun subscribe(listener: suspend (Dto36) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto36? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker36("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto36>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service36"

    override fun compareTo(other: Service36): Int = 0

    inner class Session(val token: String) {
        fun service(): Service36 = this@Service36
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 36

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo36<Dto36, Long>): Service36 = Service36(repo) { 0L }

        private val logger = Any()
    }
}

object Registry36 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

class LoggingRepo36(private val delegate: Repo36<Dto36, Long>) : Repo36<Dto36, Long> by delegate {
    override fun save(entity: Dto36): Dto36 = delegate.save(entity)
}

// FILE: p36/Utils36.kt
package bench.p36

const val MAX_ITEMS_36: Int = 1000

val defaultTags36: List<String> = listOf("a", "b")

var mutableSetting36: String? = null

fun Dto36.display(): String = name

val Dto36.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull36(): T? = getOrNull(1)

fun createDto36(id: Long, name: String = "dto", vararg tags: String): Dto36 = Dto36(id, name, tags.toList())

inline fun <reified T> typeName36(): String = T::class.java.name

suspend fun loadAll36(service: Service36): List<Dto36> = emptyList()

internal fun internalUtil36(x: Int): Int = x + 1
// FILE: p37/Model37.kt
@file:JvmName("Model37Kt")
package bench.p37

import bench.p36.Dto36
import bench.p36.Status36

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker37(val value: String, val priority: Int = 0)

@Marker37("dto", priority = 37)
data class Dto37(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto36? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto37 = copy(name = newName)
}

enum class Status37(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status37? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result37<out T> {
    data class Ok<T>(val value: T) : Result37<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result37<Nothing>
    object Loading : Result37<Nothing>
}

@JvmInline
value class Id37(val raw: Long)

interface Repo37<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService37<T : Any>(protected val repo: Repo37<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p37/Service37.kt
package bench.p37

import bench.p36.Dto36
import bench.p36.Status36

class Service37(
    repo: Repo37<Dto37, Long>,
    private val clock: () -> Long,
) : BaseService37<Dto37>(repo), Comparable<Service37> {
    override fun validate(entity: Dto37): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto37>, filter: (Dto37) -> Boolean): List<Dto37> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto37> = emptyList()

    suspend fun refresh(force: Boolean): Result37<Dto37> = Result37.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto37) -> R?): List<Dto37> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id37): Dto37? = null

    fun status(dto: Dto37): Status37 = Status37.ACTIVE

    fun copyInto(target: MutableList<in Dto37>, source: List<out Dto37>) {}

    fun subscribe(listener: suspend (Dto37) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto37? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker37("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto37>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service37"

    override fun compareTo(other: Service37): Int = 0

    inner class Session(val token: String) {
        fun service(): Service37 = this@Service37
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 37

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo37<Dto37, Long>): Service37 = Service37(repo) { 0L }

        private val logger = Any()
    }
}

object Registry37 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p37/Utils37.kt
package bench.p37

const val MAX_ITEMS_37: Int = 1000

val defaultTags37: List<String> = listOf("a", "b")

var mutableSetting37: String? = null

fun Dto37.display(): String = name

val Dto37.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull37(): T? = getOrNull(1)

fun createDto37(id: Long, name: String = "dto", vararg tags: String): Dto37 = Dto37(id, name, tags.toList())

inline fun <reified T> typeName37(): String = T::class.java.name

suspend fun loadAll37(service: Service37): List<Dto37> = emptyList()

internal fun internalUtil37(x: Int): Int = x + 1
// FILE: p38/Model38.kt
@file:JvmName("Model38Kt")
package bench.p38

import bench.p37.Dto37
import bench.p37.Status37

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker38(val value: String, val priority: Int = 0)

@Marker38("dto", priority = 38)
data class Dto38(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto37? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto38 = copy(name = newName)
}

enum class Status38(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status38? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result38<out T> {
    data class Ok<T>(val value: T) : Result38<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result38<Nothing>
    object Loading : Result38<Nothing>
}

@JvmInline
value class Id38(val raw: Long)

interface Repo38<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService38<T : Any>(protected val repo: Repo38<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p38/Service38.kt
package bench.p38

import bench.p37.Dto37
import bench.p37.Status37

class Service38(
    repo: Repo38<Dto38, Long>,
    private val clock: () -> Long,
) : BaseService38<Dto38>(repo), Comparable<Service38> {
    override fun validate(entity: Dto38): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto38>, filter: (Dto38) -> Boolean): List<Dto38> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto38> = emptyList()

    suspend fun refresh(force: Boolean): Result38<Dto38> = Result38.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto38) -> R?): List<Dto38> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id38): Dto38? = null

    fun status(dto: Dto38): Status38 = Status38.ACTIVE

    fun copyInto(target: MutableList<in Dto38>, source: List<out Dto38>) {}

    fun subscribe(listener: suspend (Dto38) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto38? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker38("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto38>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service38"

    override fun compareTo(other: Service38): Int = 0

    inner class Session(val token: String) {
        fun service(): Service38 = this@Service38
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 38

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo38<Dto38, Long>): Service38 = Service38(repo) { 0L }

        private val logger = Any()
    }
}

object Registry38 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p38/Utils38.kt
package bench.p38

const val MAX_ITEMS_38: Int = 1000

val defaultTags38: List<String> = listOf("a", "b")

var mutableSetting38: String? = null

fun Dto38.display(): String = name

val Dto38.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull38(): T? = getOrNull(1)

fun createDto38(id: Long, name: String = "dto", vararg tags: String): Dto38 = Dto38(id, name, tags.toList())

inline fun <reified T> typeName38(): String = T::class.java.name

suspend fun loadAll38(service: Service38): List<Dto38> = emptyList()

internal fun internalUtil38(x: Int): Int = x + 1
// FILE: p39/Model39.kt
@file:JvmName("Model39Kt")
package bench.p39

import bench.p38.Dto38
import bench.p38.Status38

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Marker39(val value: String, val priority: Int = 0)

@Marker39("dto", priority = 39)
data class Dto39(
    val id: Long,
    val name: String,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, Any?> = emptyMap(),
    val parent: Dto38? = null,
    var counter: Int = 0,
) {
    val displayName: String get() = "$name#$id"
    fun withName(newName: String): Dto39 = copy(name = newName)
}

enum class Status39(val code: Int) {
    ACTIVE(1),
    SUSPENDED(2) {
        override fun label(): String = "suspended"
    },
    DELETED(3);

    open fun label(): String = name.lowercase()

    companion object {
        fun byCode(code: Int): Status39? = entries.firstOrNull { it.code == code }
    }
}

sealed interface Result39<out T> {
    data class Ok<T>(val value: T) : Result39<T>
    data class Err(val error: Throwable, val retryable: Boolean = false) : Result39<Nothing>
    object Loading : Result39<Nothing>
}

@JvmInline
value class Id39(val raw: Long)

interface Repo39<T : Any, ID : Comparable<ID>> {
    fun findById(id: ID): T?
    fun findAll(limit: Int = 100, offset: Int = 0): List<T>
    fun save(entity: T): T
    fun delete(entity: T): Boolean = false
    val size: Int
    fun count(): Int = size
}

abstract class BaseService39<T : Any>(protected val repo: Repo39<T, Long>) {
    abstract fun validate(entity: T): Boolean
    open fun process(items: Collection<T>, filter: (T) -> Boolean = { true }): List<T> = items.filter(filter)
    protected open val serviceName: String get() = "base"
    internal fun internalHelper(value: Int): Int = value
}

// FILE: p39/Service39.kt
package bench.p39

import bench.p38.Dto38
import bench.p38.Status38

class Service39(
    repo: Repo39<Dto39, Long>,
    private val clock: () -> Long,
) : BaseService39<Dto39>(repo), Comparable<Service39> {
    override fun validate(entity: Dto39): Boolean = entity.name.isNotEmpty()

    override fun process(items: Collection<Dto39>, filter: (Dto39) -> Boolean): List<Dto39> = super.process(items, filter)

    @JvmOverloads
    fun find(name: String, limit: Int = 10, includeDeleted: Boolean = false): List<Dto39> = emptyList()

    suspend fun refresh(force: Boolean): Result39<Dto39> = Result39.Loading

    fun <R : Comparable<R>> sortedBy(selector: (Dto39) -> R?): List<Dto39> = emptyList()

    fun log(level: Int, vararg messages: String) {}

    fun findByTypedId(id: Id39): Dto39? = null

    fun status(dto: Dto39): Status39 = Status39.ACTIVE

    fun copyInto(target: MutableList<in Dto39>, source: List<out Dto39>) {}

    fun subscribe(listener: suspend (Dto39) -> Unit): AutoCloseable = AutoCloseable {}

    @Deprecated("Use find")
    fun legacy(): Dto39? = null

    @JvmName("renamedForJava")
    fun renamed(): Int = 0

    @Marker39("op")
    fun annotated(@Suppress("UNUSED_PARAMETER") input: String?): String? = input

    @Volatile
    var lastRun: Long = 0

    lateinit var cache: MutableMap<Long, Dto39>

    val stats: Map<String, Int> by lazy { emptyMap() }

    var mode: String = "default"
        private set

    override val serviceName: String get() = "service39"

    override fun compareTo(other: Service39): Int = 0

    inner class Session(val token: String) {
        fun service(): Service39 = this@Service39
    }

    class Config(val limit: Int = 10, val enabled: Boolean = true)

    companion object {
        const val VERSION: Int = 39

        @JvmField
        val DEFAULT_LIMIT: Int = 100

        @JvmStatic
        fun create(repo: Repo39<Dto39, Long>): Service39 = Service39(repo) { 0L }

        private val logger = Any()
    }
}

object Registry39 {
    private val items = mutableListOf<Any>()
    fun register(item: Any) { items += item }
    val count: Int get() = items.size
    @JvmStatic fun reset() { items.clear() }
}

// FILE: p39/Utils39.kt
package bench.p39

const val MAX_ITEMS_39: Int = 1000

val defaultTags39: List<String> = listOf("a", "b")

var mutableSetting39: String? = null

fun Dto39.display(): String = name

val Dto39.isRoot: Boolean get() = parent == null

fun <T> List<T>.secondOrNull39(): T? = getOrNull(1)

fun createDto39(id: Long, name: String = "dto", vararg tags: String): Dto39 = Dto39(id, name, tags.toList())

inline fun <reified T> typeName39(): String = T::class.java.name

suspend fun loadAll39(service: Service39): List<Dto39> = emptyList()

internal fun internalUtil39(x: Int): Int = x + 1
