interface rawTypeWithBound {
    interface Super<T extends Integer> {
        T typeForSubstitute();
    }

    interface MidRaw extends Super {
    }
}