function ExternalClassInheritedFromAbstractExternalClass() {}
ExternalClassInheritedFromAbstractExternalClass.prototype = Object.create(AbstractExternalClass.prototype);
ExternalClassInheritedFromAbstractExternalClass.prototype.abstractFunction = function() {
    return "ExternalClassInheritedFromAbstractExternalClass.abstractFunction";
}
ExternalClassInheritedFromAbstractExternalClass.prototype.removedAbstractFunction = function() {
    return "ExternalClassInheritedFromAbstractExternalClass.removedAbstractFunction";
}

function OpenExternalClass() {}
OpenExternalClass.prototype.function = function() {
    return "OpenExternalClass.function"
}

function ExternalInterfaceInheritedFromOpenExternalClass() {}
ExternalInterfaceInheritedFromOpenExternalClass.prototype = Object.create(OpenExternalClass.prototype);
ExternalInterfaceInheritedFromOpenExternalClass.prototype.abstractFunction = function() {
    throw new Error("Calling abstract function ExternalInterfaceInheritedFromOpenExternalClass.abstractFunction");
}

function ExternalClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass() {}
ExternalClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass.prototype = Object.create(ExternalInterfaceInheritedFromOpenExternalClass.prototype);
ExternalClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass.prototype.abstractFunction = function() {
    return "ExternalClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass.abstractFunction"
}
