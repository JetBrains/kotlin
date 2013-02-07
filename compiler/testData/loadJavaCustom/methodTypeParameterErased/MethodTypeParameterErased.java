package test;

public interface MethodTypeParameterErased {

    public interface Bug<T> {
            <RET extends Bug<T>> RET save();
            void foo();
    }

    public abstract class SubBug implements Bug<Object> {
            public SubBug save() {
                    return this;
            }

            public void foo() {}

    }

}