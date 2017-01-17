(function(global) {
    var modules = {};
    modules.kotlin = kotlin;

    function define(name, dependencies, body) {
        var resolvedDependencies = [];
        var currentModule = {};
        modules[name] = currentModule;
        for (var i = 0; i < dependencies.length; ++i) {
            var dependencyName = dependencies[i];
            resolvedDependencies[i] = dependencyName === 'exports' ? currentModule : modules[dependencyName];
        }
        currentModule = body.apply(body, resolvedDependencies);
        if (currentModule) {
            modules[name] = currentModule;
        }
    }
    define.amd = {};

    function require(name) {
        return modules[name];
    }

    global.define = define;
    global.$kotlin_test_internal$ = {
        require : require
    };
})(this);