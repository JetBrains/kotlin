// FILE: RunnerModule.kt

class RunnerModule : AbstractModule() {
    fun configure() {
        <expr>install(CoroutineModule.builder())</expr>
    }
}

// FILE: Module.java

public interface Module {
    public interface Builder {
        Module build();
    }
}

// FILE: AbstractModule.java

public abstract class AbstractModule {
    public abstract static class Builder<B extends Builder<B>> implements Module.Builder {
        protected abstract B self();
        public abstract Module build();
    }

    protected void install(Builder<?> builder) {}
}

// FILE: CoroutineModule.kt

class CoroutineModule : AbstractModule() {
    class Builder : AbstractModule.Builder<Builder>() {
        protected override fun self(): Builder = this
        override fun build(): Module = CoroutineModule()
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
