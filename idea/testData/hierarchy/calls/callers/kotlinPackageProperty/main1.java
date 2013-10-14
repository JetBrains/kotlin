class JA {
    public String name = _DefaultPackage.getPackageVal();

    public String getName() {
        _DefaultPackage.setPackageVal("");
        return _DefaultPackage.getPackageVal();
    }
}