function OpenExternalClass() {}
OpenExternalClass.prototype.function = function() {
    return "OpenExternalClass.function"
}

function ExternalInterfaceInheritedFromOpenExternalClass() {}
ExternalInterfaceInheritedFromOpenExternalClass.prototype = Object.create(OpenExternalClass.prototype);
ExternalInterfaceInheritedFromOpenExternalClass.prototype.abstractFunction = function() {
    throw new Error("Calling abstract function ExternalInterfaceInheritedFromOpenExternalClass.abstractFunction");
}
