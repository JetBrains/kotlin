interface rawTypeWithSelfReferenceBound {
    interface Super<T extends Super<T>> {
        T typeForSubstitute();
    }

    interface MidRaw extends Super {
    }
}