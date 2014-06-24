interface rawType {
    interface Super<T> {
        T typeForSubstitute();
    }

    interface MidRaw extends Super {
    }
}