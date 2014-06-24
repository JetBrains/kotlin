import org.jetbrains.annotations.NotNull;

class Container {
    @NotNull
    Value get(Runnable i) {
        i.run();
        return new Value();
    }

    void set(Runnable i, @NotNull Value value) {
        i.run();
    }
}

class Value {
    @NotNull Value plus(Runnable i) {
        i.run();
        return this;
    }

    @NotNull Value minus(Runnable i) {
        i.run();
        return this;
    }

    @NotNull Value times(Runnable i) {
        i.run();
        return this;
    }

    @NotNull Value div(Runnable i) {
        i.run();
        return this;
    }

    @NotNull Value mod(Runnable i) {
        i.run();
        return this;
    }
}
