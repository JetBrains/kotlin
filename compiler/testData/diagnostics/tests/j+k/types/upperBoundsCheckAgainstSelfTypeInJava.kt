// FIR_IDENTICAL
// SKIP_TXT

// FILE: XBreakpointProperties.java
public abstract class XBreakpointProperties<T> {}
// FILE: XBreakpoint.java
public interface XBreakpoint<P extends XBreakpointProperties> {}
// FILE: XBreakpointType.java
public abstract class XBreakpointType<B extends XBreakpoint<P>, P extends XBreakpointProperties> {}

// FILE: main.kt
fun foo(x: XBreakpointType<XBreakpoint<*>, *>) {}
