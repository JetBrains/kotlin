(function(global) {
    var modules = {};
    modules.kotlin = kotlin;

    function define(name, dependencies, body) {
        var resolvedDependencies = [];
        for (var i = 0; i < dependencies.length; ++i) {
            resolvedDependencies[i] = modules[dependencies[i]];
        }
        modules[name] = body.apply(body, resolvedDependencies);
    }
    define.amd = {};

    global.define = define;
})(this);