package research.kapt.base

/**
 * Hand-written on purpose: gradle-profiler's `apply-abi-change-to` must not target a generated file,
 * because `generateSources` owns that directory as a task output and would restore the mutation.
 *
 * Every generated class references this type when `-PcommonBase=true`, so one ABI change here
 * impacts all N types — the regime kapt's incremental bookkeeping is quadratic in.
 */
open class Base {
    open fun id(): Int = 0
}
