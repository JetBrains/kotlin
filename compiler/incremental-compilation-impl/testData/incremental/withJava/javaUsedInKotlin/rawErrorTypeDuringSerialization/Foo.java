// WITH_STDLIB
// TARGET_BACKEND: JVM

/*
 ServletOpenApiContextBuilder<
    T : raw (
        ServletOpenApiContextBuilder<ServletOpenApiContextBuilder<*>>
        ..
        ServletOpenApiContextBuilder<out ServletOpenApiContextBuilder<*>>?
    )
 >
 : GenericOpenApiContextBuilder<
        raw (
            ServletOpenApiContextBuilder<[ERROR : <LOOP IN SUPERTYPES>]> // exception on this error type during serialization
            ..
            ServletOpenApiContextBuilder<*>?
        )
    >
 */
public class Foo<T extends Foo> extends Bar<Foo> {
    String ctxId = "";

    public T ctxId(String ctxId) {
        this.ctxId = ctxId;
        return (T) this;
    }
}
