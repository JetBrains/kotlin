class A {
    @javax.annotation.Nullable
    String nullable() { return null; }

    @javax.annotation.CheckForNull
    String checkForNull() { return null; }

    @javax.annotation.Nonnull
    String nonNull() { return null; }

    @javax.annotation.Nonnull(when = javax.annotation.meta.When.ALWAYS)
    String nonNullExplicitArgument() { return null; }
}
