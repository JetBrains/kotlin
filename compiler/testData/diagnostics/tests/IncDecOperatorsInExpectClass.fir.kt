// !LANGUAGE: +MultiPlatformProjects
// SKIP_TXT
// Issue: KT-49714

expect class Counter {
    operator fun inc(): Counter
    operator fun dec(): Counter
}

actual typealias Counter = Int
