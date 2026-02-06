var kotlin;
var emulatedModules = { kotlin: kotlin };
var module = { exports: {} };
var currentModuleId;


// TODO don't expose by default when run test with AMD module kind
function require(moduleId) {
    return emulatedModules[moduleId];
}

var $kotlin_test_internal$ = {
    require: require,
    beginModule : function () {
        module.exports = {};
    },
    endModule : function(moduleId) {
        emulatedModules[moduleId] = module.exports;
    },
    setModuleId: function(moduleId) {
        currentModuleId = moduleId;
    }
};

// TODO expose only when run test with AMD or UMD module kind
function define(moduleId, dependencies, body) {
    if (Array.isArray(moduleId)) {
        body = dependencies;
        dependencies = moduleId;
        moduleId = currentModuleId;
    }
    var resolvedDependencies = [];
    emulatedModules[moduleId] = {};
    for (var i = 0; i < dependencies.length; ++i) {
        var dependencyName = dependencies[i];
        resolvedDependencies.push(emulatedModules[dependencyName === 'exports' ? moduleId : dependencyName]);
    }
    var result = body.apply(null, resolvedDependencies);
    if (result != null) {
        emulatedModules[moduleId] = result;
    }
}
define.amd = {};
