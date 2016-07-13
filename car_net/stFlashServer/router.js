/**
 * Created by user on 7/11/16.
 */
function route(handle, pathname, httpContent, response) {
    if (typeof handle[pathname] === 'function') {
        handle[pathname](httpContent, response);
    } else {
        console.log("No request handler found for " + pathname);
        response.writeHead(404, {"Content-Type": "text/plain"});
        response.write("404 Not found");
        response.end();
    }
}
exports.route = route;