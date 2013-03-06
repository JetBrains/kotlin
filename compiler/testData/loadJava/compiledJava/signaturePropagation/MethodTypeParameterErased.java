package test;

public interface MethodTypeParameterErased {

    public interface Bug<T> {
            <RET extends Bug<T>> RET save();
    }

    public class SubBug implements Bug<Object> {
            public SubBug save() {
                    return this;
            }
    }

}