fun foo(thread: Thread) {
    thread.get<caret>
}

// EXIST_JAVA_ONLY: getPriority
