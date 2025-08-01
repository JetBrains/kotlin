// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79777

// FILE: XBreakpointProperties.java
public interface XBreakpointProperties<T> {}

// FILE: XBreakpoint.java
public interface XBreakpoint<P extends XBreakpointProperties> {}

// FILE: XBreakpointType.java
public interface XBreakpointType<B extends XBreakpoint<P>, P extends XBreakpointProperties> {}

// FILE: XBreakpointImpl.java
public class XBreakpointImpl<S extends XBreakpoint<P>, P extends XBreakpointProperties> {
    public XBreakpointType<S, P> getType() {
        return null;
    }
}

// FILE: test.kt

fun <B: XBreakpoint<P>, P: XBreakpointProperties<*>> consume(
    breakpointType: XBreakpointType<B, P>,
) {}

fun test(breakpoint: XBreakpointImpl<*, *>) {
    consume(
        breakpoint.type
    )
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaProperty, javaType, outProjection, starProjection,
typeConstraint, typeParameter */
