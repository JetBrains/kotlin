// TARGET_BACKEND: JVM
package test

var Long.date1: Any get() = java.util.Date()
    set(value) {}

var Long.date3: Any get() = java.util.Date()
    private set(value) {}

private var Long.date4: java.util.Date get() = java.util.Date()
    set(value) {}

public var Long.date7: java.util.Date get() = java.util.Date()
    set(value) {}

public var Long.date8: java.util.Date get() = java.util.Date()
    internal set(value) {}

public var Long.date9: java.util.Date get() = java.util.Date()
    private set(value) {}

public var Long.date11: java.util.Date get() = java.util.Date()
    public set(value) {}
